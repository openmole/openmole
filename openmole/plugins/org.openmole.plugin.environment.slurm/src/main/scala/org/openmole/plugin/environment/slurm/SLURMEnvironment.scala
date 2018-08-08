/*
 * Copyright (C) 2012 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.environment.slurm

import org.openmole.core.authentication._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants.{Time, _}
import squants.information._
import effectaside._
import org.openmole.plugin.environment.batch.storage.StorageInterface
import org.openmole.plugin.environment.gridscale._

object SLURMEnvironment {

    def apply(
      user:                 OptionalArgument[String]      = None,
      host:                 OptionalArgument[String]      = None,
      port:                 OptionalArgument[Int]         = 22,
      queue:                OptionalArgument[String]      = None,
      openMOLEMemory:       OptionalArgument[Information] = None,
      wallTime:             OptionalArgument[Time]        = None,
      memory:               OptionalArgument[Information] = None,
      qos:                  OptionalArgument[String]      = None,
      gres:                 Seq[Gres]                     = List(),
      constraints:          Seq[String]                   = List(),
      nodes:                OptionalArgument[Int]         = None,
      coresByNode:          OptionalArgument[Int]         = None,
      sharedDirectory:      OptionalArgument[String]      = None,
      workDirectory:        OptionalArgument[String]      = None,
      threads:              OptionalArgument[Int]         = None,
      timeout:              OptionalArgument[Time]        = None,
      storageSharedLocally: Boolean                       = false,
      name:                 OptionalArgument[String]      = None,
      localSubmission:      Boolean                       = false,
      forceCopyOnNode:      Boolean                       = false
    )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
      import services._

      val parameters = Parameters(
        queue = queue,
        openMOLEMemory = openMOLEMemory,
        wallTime = wallTime,
        memory = memory,
        qos = qos,
        gres = gres,
        constraints = constraints,
        nodes = nodes,
        coresByNode = coresByNode,
        sharedDirectory = sharedDirectory,
        workDirectory = workDirectory,
        threads = threads,
        storageSharedLocally = storageSharedLocally,
        forceCopyOnNode = forceCopyOnNode)

      EnvironmentProvider { () =>
        if (!localSubmission) {
          val userValue = user.mustBeDefined("user")
          val hostValue = host.mustBeDefined("host")
          val portValue = port.mustBeDefined("port")

          new SLURMEnvironment(
            user = userValue,
            host = hostValue,
            port = portValue,
            timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
            parameters = parameters,
            name = Some(name.getOrElse(varName.value)),
            authentication = SSHAuthentication.find(userValue, hostValue, portValue)
          )
        } else
          new SLURMLocalEnvironment(
            parameters = parameters,
            name = Some(name.getOrElse(varName.value))
          )
      }

    }



  implicit def asSSHServer[A: gridscale.ssh.SSHAuthentication]: AsSSHServer[SLURMEnvironment[A]] = new AsSSHServer[SLURMEnvironment[A]] {
    override def apply(t: SLURMEnvironment[A]) = gridscale.ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  implicit def isJobService[A]: JobServiceInterface[SLURMEnvironment[A]] = new JobServiceInterface[SLURMEnvironment[A]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: SLURMEnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: SLURMEnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SLURMEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: SLURMEnvironment[A], j: J) = js.stdOutErr(j)
  }

  case class Parameters(
    queue:                Option[String],
    openMOLEMemory:       Option[Information],
    wallTime:             Option[Time],
    memory:               Option[Information],
    qos:                  Option[String],
    gres:                 Seq[Gres],
    constraints:          Seq[String],
    nodes:                Option[Int],
    coresByNode:          Option[Int],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean,
    forceCopyOnNode: Boolean)

}

class SLURMEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     SLURMEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment {
  env ⇒

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

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = StorageInterface.remote(LogicalLinkStorage(forceCopy = parameters.forceCopyOnNode))
    BatchEnvironment.serializeJob(storageService, remoteStorage, batchExecutionJob)
  }

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

    val description = _root_.gridscale.slurm.SLURMJobDescription(
      command = s"/bin/bash $remoteScript",
      queue = parameters.queue,
      workDirectory = workDirectory,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coresByNode = parameters.coresByNode orElse parameters.threads,
      qos = parameters.qos,
      gres = parameters.gres.toList,
      constraints = parameters.constraints.toList
    )

    val id = gridscale.slurm.submit[_root_.gridscale.ssh.SSHServer](env, description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.slurm.state[_root_.gridscale.ssh.SSHServer](env, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.slurm.clean[_root_.gridscale.ssh.SSHServer](env, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.slurm.stdOut[_root_.gridscale.ssh.SSHServer](env, id), gridscale.slurm.stdErr[_root_.gridscale.ssh.SSHServer](env, id))

  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxConnections))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)

}



object SLURMLocalEnvironment {
  implicit def isJobService: JobServiceInterface[SLURMLocalEnvironment] = new JobServiceInterface[SLURMLocalEnvironment] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: SLURMLocalEnvironment, serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: SLURMLocalEnvironment, j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SLURMLocalEnvironment, j: J): Unit = env.delete(j)
    override def stdOutErr(js: SLURMLocalEnvironment, j: J) = js.stdOutErr(j)
  }
}


class SLURMLocalEnvironment(
  val parameters:     SLURMEnvironment.Parameters,
  val name:           Option[String])(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env =>

  import services._

  lazy val usageControl = UsageControl(services.preference(SSHEnvironment.MaxLocalOperations))
  def usageControls = List(storageService.usageControl, jobService.usageControl)

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def usageControls = List(storageService.usageControl, jobService.usageControl)
    BatchEnvironment.clean(this, usageControls)
  }

  lazy val storageService =
    localStorageService(
      environment = env,
      concurrency = services.preference(SSHEnvironment.MaxLocalOperations),
      root = "",
      sharedDirectory = parameters.sharedDirectory,
    )

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = StorageInterface.remote(LogicalLinkStorage())
    BatchEnvironment.serializeJob(storageService, remoteStorage, batchExecutionJob)
  }

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

    val description = _root_.gridscale.slurm.SLURMJobDescription(
      command = s"/bin/bash $remoteScript",
      queue = parameters.queue,
      workDirectory = workDirectory,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coresByNode = parameters.coresByNode orElse parameters.threads,
      qos = parameters.qos,
      gres = parameters.gres.toList,
      constraints = parameters.constraints.toList
    )

    val id = gridscale.slurm.submit(LocalHost(), description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.slurm.state(LocalHost(), id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.slurm.clean(LocalHost(), id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.slurm.stdOut(LocalHost(), id), gridscale.slurm.stdErr(LocalHost(), id))


  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxLocalOperations))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)

}



//
//class SLURMEnvironment(
//  val user:                    String,
//  val host:                    String,
//  override val port:           Int,
//  val queue:                   Option[String],
//  override val openMOLEMemory: Option[Information],
//  val wallTime:                Option[Time],
//  val memory:                  Option[Information],
//  val qos:                     Option[String],
//  val gres:                    Seq[Gres],
//  val constraints:             Seq[String],
//  val nodes:                   Option[Int],
//  val coresByNode:             Option[Int],
//  val sharedDirectory:         Option[String],
//  val workDirectory:           Option[String],
//  override val threads:        Option[Int],
//  val storageSharedLocally:    Boolean,
//  override val name:           Option[String]
//)(val credential: fr.iscpif.gridscale.ssh.SSHAuthentication)(implicit val services: BatchEnvironment.Services) extends ClusterEnvironment { env ⇒
//
//  type JS = SLURMJobService
//
//  lazy val jobService =
//    new SLURMJobService {
//      def queue = env.queue
//      val environment = env
//      def sharedFS = storage
//      def workDirectory = env.workDirectory
//      def timeout = env.timeout
//      def credential = env.credential
//      def user = env.user
//      def host = env.host
//      def port = env.port
//    }
//
//}
