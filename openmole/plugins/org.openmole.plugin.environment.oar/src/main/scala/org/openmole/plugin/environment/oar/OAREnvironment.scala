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

package org.openmole.plugin.environment.oar

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
import org.openmole.plugin.environment.gridscale._

object OAREnvironment {

  def apply(
    user:                 OptionalArgument[String]      = None,
    host:                 OptionalArgument[String]      = None,
    port:                 OptionalArgument[Int]         = 22,
    queue:                OptionalArgument[String]      = None,
    core:                 OptionalArgument[Int]         = None,
    cpu:                  OptionalArgument[Int]         = None,
    wallTime:             OptionalArgument[Time]        = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None,
    bestEffort:           Boolean                       = true,
    timeout:              OptionalArgument[Time]        = None,
    localSubmission: Boolean                            = false
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._

    val parameters = Parameters(
      queue = queue,
      core = core,
      cpu = cpu,
      wallTime = wallTime,
      openMOLEMemory = openMOLEMemory,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      bestEffort = bestEffort
    )

    EnvironmentProvider { () =>
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new OAREnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue)
        )
      } else
        new OARLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value))
        )
    }
  }

  implicit def asSSHServer[A: gridscale.ssh.SSHAuthentication]: AsSSHServer[OAREnvironment[A]] = new AsSSHServer[OAREnvironment[A]] {
    override def apply(t: OAREnvironment[A]) = gridscale.ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  implicit def isJobService[A]: JobServiceInterface[OAREnvironment[A]] = new JobServiceInterface[OAREnvironment[A]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: OAREnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: OAREnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: OAREnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: OAREnvironment[A], j: J) = js.stdOutErr(j)
  }

  case class Parameters(
    queue:                Option[String],
    core:                 Option[Int],
    cpu:                  Option[Int],
    wallTime:             Option[Time],
    openMOLEMemory:       Option[Information],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean,
    bestEffort:           Boolean
  )

  def nbCores(parameters: Parameters) = parameters.core orElse parameters.threads

}

class OAREnvironment[A: gridscale.ssh.SSHAuthentication](
    val parameters:     OAREnvironment.Parameters,
    val user:           String,
    val host:           String,
    val port:           Int,
    val timeout:        Time,
    val name:           Option[String],
    val authentication: A
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def usageControls = List(storageService.usageControl, jobService.usageControl)
    try BatchEnvironment.clean(this, usageControls)
    finally sshInterpreter().close
  }

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  lazy val storageService =
    sshStorageService(
      user = user,
      host = host,
      port = port,
      storage = env,
      environment = env,
      concurrency = services.preference(SSHEnvironment.MaxConnections),
      sharedDirectory = parameters.sharedDirectory,
      storageSharedLocally = parameters.storageSharedLocally
    )

  override def serializeJob(batchExecutionJob: BatchExecutionJob) =
    BatchEnvironment.serializeJob(storageService, batchExecutionJob)

  val installRuntime = new RuntimeInstallation(
    Frontend.ssh(host, port, timeout, authentication),
    storageService = storageService
  )

  def submit(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      import services._
      SharedStorage.buildScript(
        env.installRuntime.apply,
        parameters.workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        env.storageService
      )
    }

    val (remoteScript, result, workDirectory) = buildScript(serializedJob)
    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = OAREnvironment.nbCores(parameters),
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    val id = gridscale.oar.submit[_root_.gridscale.ssh.SSHServer](env, description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.oar.state[_root_.gridscale.ssh.SSHServer](env, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.oar.clean[_root_.gridscale.ssh.SSHServer](env, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.oar.stdOut[_root_.gridscale.ssh.SSHServer](env, id), gridscale.oar.stdErr[_root_.gridscale.ssh.SSHServer](env, id))

  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxConnections))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)

}

object OARLocalEnvironment{
  implicit def isJobService: JobServiceInterface[OARLocalEnvironment] = new JobServiceInterface[OARLocalEnvironment] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: OARLocalEnvironment, serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: OARLocalEnvironment, j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: OARLocalEnvironment, j: J): Unit = env.delete(j)
    override def stdOutErr(js: OARLocalEnvironment, j: J) = js.stdOutErr(j)
  }
}

class OARLocalEnvironment(
    val parameters: OAREnvironment.Parameters,
    val name:       Option[String]
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  lazy val usageControl = UsageControl(services.preference(SSHEnvironment.MaxLocalOperations))
  def usageControls = List(storageService.usageControl, jobService.usageControl)

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  lazy val storageService =
   localStorageService(
      environment = env,
      concurrency = services.preference(SSHEnvironment.MaxLocalOperations),
      root = "",
      sharedDirectory = parameters.sharedDirectory,
    )

  override def serializeJob(batchExecutionJob: BatchExecutionJob) =
    BatchEnvironment.serializeJob(storageService, batchExecutionJob)

  val installRuntime = new RuntimeInstallation(
    Frontend.local,
    storageService = storageService
  )

  import _root_.gridscale.local.LocalHost

  def submit(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      import services._
      SharedStorage.buildScript(
        env.installRuntime.apply,
        parameters.workDirectory,
        parameters.openMOLEMemory,
        parameters.threads,
        serializedJob,
        env.storageService
      )
    }

    val (remoteScript, result, workDirectory) = buildScript(serializedJob)
    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = OAREnvironment.nbCores(parameters),
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    val id = gridscale.oar.submit(LocalHost(), description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.oar.state(LocalHost(), id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.oar.clean(LocalHost(), id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.oar.stdOut(LocalHost(), id), gridscale.oar.stdErr(LocalHost(), id))


  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxLocalOperations))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def usageControls = List(storageService.usageControl, jobService.usageControl)
    BatchEnvironment.clean(this, usageControls)
  }
}
