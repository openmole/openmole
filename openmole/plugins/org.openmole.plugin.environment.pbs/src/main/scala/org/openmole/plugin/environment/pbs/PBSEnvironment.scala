/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.pbs

import org.openmole.core.authentication._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._
import effectaside._
import gridscale.cluster.HeadNode
import org.openmole.core.preference.Preference
import org.openmole.plugin.environment.batch.storage.{ HierarchicalStorageInterface, QualityControl, StorageInterface }
import org.openmole.plugin.environment.gridscale.{ GridScaleJobService, LocalStorage, LogicalLinkStorage }
import org.openmole.plugin.environment.pbs.PBSEnvironment.Parameters
import org.openmole.tool.logger.JavaLogger

object PBSEnvironment extends JavaLogger {

  def apply(
    user:                 OptionalArgument[String]      = None,
    host:                 OptionalArgument[String]      = None,
    port:                 OptionalArgument[Int]         = 22,
    queue:                OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    wallTime:             OptionalArgument[Time]        = None,
    memory:               OptionalArgument[Information] = None,
    nodes:                OptionalArgument[Int]         = None,
    coreByNode:           OptionalArgument[Int]         = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    timeout:              OptionalArgument[Time]        = None,
    flavour:              gridscale.pbs.PBSFlavour      = Torque,
    name:                 OptionalArgument[String]      = None,
    localSubmission:      Boolean                       = false
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._

    import services._

    val parameters = Parameters(
      queue = queue,
      wallTime = wallTime,
      openMOLEMemory = openMOLEMemory,
      memory = memory,
      nodes = nodes,
      coreByNode = coreByNode,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      flavour = flavour
    )

    EnvironmentProvider { () ⇒
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new PBSEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue)
        )
      }
      else
        new PBSLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value))
        )
    }
  }

  case class Parameters(
    queue:                Option[String],
    wallTime:             Option[Time],
    openMOLEMemory:       Option[Information],
    memory:               Option[Information],
    nodes:                Option[Int],
    coreByNode:           Option[Int],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean,
    flavour:              _root_.gridscale.pbs.PBSFlavour)
}

class PBSEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     PBSEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment {
  env ⇒
  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def usageControls = List(storageService.usageControl, pbsJobService.usageControl)
    try BatchEnvironment.clean(this, usageControls)
    finally sshInterpreter().close
  }

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  lazy val usageControl = UsageControl(preference(SSHEnvironment.MaxConnections))
  lazy val qualityControl = QualityControl(preference(BatchEnvironment.QualityHysteresis))

  lazy val sshServer = gridscale.ssh.SSHServer(host, port, timeout)(authentication)

  lazy val storageService =
    if (parameters.storageSharedLocally) localStorageService(LocalStorage(UsageControl(preference(SSHEnvironment.MaxConnections)), qualityControl), env, parameters.sharedDirectory)
    else sshStorageService(
      user = user,
      host = host,
      port = port,
      storage = SSHStorageServer(sshServer, usageControl, qualityControl),
      environment = env,
      sharedDirectory = parameters.sharedDirectory,
      storageSharedLocally = parameters.storageSharedLocally
    )

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage())
    BatchEnvironment.serializeJob(storageService, remoteStorage, batchExecutionJob)
  }

  val installRuntime = new RuntimeInstallation(
    Frontend.ssh(host, port, timeout, authentication),
    storageService = storageService
  )

  lazy val pbsJobService =
    if (parameters.storageSharedLocally) new PBSJobService(LocalStorage(usageControl, qualityControl), installRuntime, parameters, sshServer, usageControl)
    else new PBSJobService(SSHStorageServer(sshServer, usageControl, qualityControl), installRuntime, parameters, sshServer, usageControl)

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(pbsJobService, serializedJob)
}

class PBSLocalEnvironment(
  val parameters: PBSEnvironment.Parameters,
  val name:       Option[String]
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val localInterpreter = gridscale.local.Local()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def usageControls = List(storageService.usageControl, pbsJobService.usageControl)
    BatchEnvironment.clean(this, usageControls)
  }

  lazy val storage = LocalStorage(UsageControl(preference(SSHEnvironment.MaxLocalOperations)), QualityControl(preference(BatchEnvironment.QualityHysteresis)))

  lazy val usageControl = UsageControl(preference(SSHEnvironment.MaxConnections))
  lazy val qualityControl = QualityControl(preference(BatchEnvironment.QualityHysteresis))

  lazy val storageService =
    localStorageService(
      storage = LocalStorage(usageControl, qualityControl),
      environment = env,
      sharedDirectory = parameters.sharedDirectory)

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage())
    BatchEnvironment.serializeJob(storageService, remoteStorage, batchExecutionJob)
  }

  val installRuntime = new RuntimeInstallation(
    Frontend.local,
    storageService = storageService
  )

  import _root_.gridscale.local.LocalHost

  lazy val pbsJobService = new PBSJobService(LocalStorage(usageControl, qualityControl), installRuntime, parameters, LocalHost(), UsageControl(preference(SSHEnvironment.MaxConnections)))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(pbsJobService, serializedJob)

}

object PBSJobService {

  implicit def isJobService[A, B]: JobServiceInterface[PBSJobService[A, B]] = new JobServiceInterface[PBSJobService[A, B]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: PBSJobService[A, B], serializedJob: SerializedJob): J = env.submit(serializedJob)
    override def state(env: PBSJobService[A, B], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: PBSJobService[A, B], j: J): Unit = env.delete(j)
    override def stdOutErr(js: PBSJobService[A, B], j: J) = js.stdOutErr(j)
    override def usageControl(js: PBSJobService[A, B]): UsageControl = js.usageControl
  }

}

class PBSJobService[S, H](
  s:                S,
  installation:     RuntimeInstallation,
  parameters:       Parameters,
  h:                H,
  val usageControl: UsageControl)(implicit storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], headNode: HeadNode[H], services: BatchEnvironment.Services) {

  import services._
  implicit val systemInterpreter = effectaside.System()

  def submit(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      SharedStorage.buildScript(
        installation.apply,
        parameters.workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        s
      )
    }

    val (remoteScript, workDirectory) = buildScript(serializedJob)

    val description = gridscale.pbs.PBSJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coreByNode = parameters.coreByNode orElse parameters.threads,
      flavour = parameters.flavour
    )

    import PBSEnvironment.Log._

    log(FINE, s"""Submitting PBS job, PBS script:
                 |${gridscale.pbs.impl.toScript(description)("uniqId")}
                 |bash script:
                 |$remoteScript""".stripMargin)

    val id = gridscale.pbs.submit(h, description)

    log(FINE, s"""Submitted PBS job with PBS script:
                 |uniqId: ${id.uniqId}
                 |job id: ${id.jobId}""".stripMargin)

    id
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.pbs.state(h, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.pbs.clean(h, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.pbs.stdOut(h, id), gridscale.pbs.stdErr(h, id))
}