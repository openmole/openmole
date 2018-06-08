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
import org.openmole.plugin.environment.batch.refresh.{JobManager, StopEnvironment}
import org.openmole.plugin.environment.gridscale._
import org.openmole.tool.logger.JavaLogger

object PBSEnvironment extends JavaLogger {

    def apply(
      user:                 OptionalArgument[String] = None,
      host:                 OptionalArgument[String] = None,
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
      flavour:              gridscale.pbs.PBSFlavour = Torque,
      name:                 OptionalArgument[String]      = None,
      localSubmission: Boolean                            = false
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

      if(!localSubmission) {
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
        authentication = SSHAuthentication.find(userValue, hostValue, portValue).apply
      )
    } else
      new PBSLocalEnvironment(
        parameters = parameters,
        name = Some(name.getOrElse(varName.value))
      )


    }

  implicit def asSSHServer[A: gridscale.ssh.SSHAuthentication]: AsSSHServer[PBSEnvironment[A]] = new AsSSHServer[PBSEnvironment[A]] {
    override def apply(t: PBSEnvironment[A]) = gridscale.ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  implicit def isJobService[A]: JobServiceInterface[PBSEnvironment[A]] = new JobServiceInterface[PBSEnvironment[A]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: PBSEnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: PBSEnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: PBSEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: PBSEnvironment[A], j: J) = js.stdOutErr(j)
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
    flavour:              gridscale.pbs.PBSFlavour)
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

  override def stop() =
    try super.stop()
    finally {
      def usageControls = List(storageService.usageControl, jobService.usageControl)
      JobManager ! StopEnvironment(this, usageControls, Some(sshInterpreter().close))
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

  override def trySelectStorage(files: ⇒ Vector[File]) = BatchEnvironment.trySelectSingleStorage(storageService)

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
    
    val id = gridscale.pbs.submit[_root_.gridscale.ssh.SSHServer](env, description)

    log(FINE, s"""Submitted PBS job with PBS script:
      |uniqId: ${id.uniqId}
      |job id: ${id.jobId}""".stripMargin)

    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.pbs.state[_root_.gridscale.ssh.SSHServer](env, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.pbs.clean[_root_.gridscale.ssh.SSHServer](env, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.pbs.stdOut[_root_.gridscale.ssh.SSHServer](env, id), gridscale.pbs.stdErr[_root_.gridscale.ssh.SSHServer](env, id))

  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxConnections))
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)

}

object PBSLocalEnvironment{
  implicit def isJobService: JobServiceInterface[PBSLocalEnvironment] = new JobServiceInterface[PBSLocalEnvironment] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: PBSLocalEnvironment, serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: PBSLocalEnvironment, j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: PBSLocalEnvironment, j: J): Unit = env.delete(j)
    override def stdOutErr(js: PBSLocalEnvironment, j: J) = js.stdOutErr(j)
  }
}

class PBSLocalEnvironment(
   val parameters: PBSEnvironment.Parameters,
   val name:       Option[String]
  )(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  lazy val usageControl = UsageControl(services.preference(SSHEnvironment.MaxLocalOperations))
  def usageControls = List(storageService.usageControl, jobService.usageControl)

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  override def stop() =
    try super.stop()
    finally  {
      def usageControls = List(storageService.usageControl, jobService.usageControl)
      JobManager ! StopEnvironment(this, usageControls)
    }

  lazy val storageService =
    localStorageService(
      environment = env,
      concurrency = services.preference(SSHEnvironment.MaxLocalOperations),
      root = "",
      sharedDirectory = parameters.sharedDirectory,
    )


  override def trySelectStorage(files: ⇒ Vector[File]) = BatchEnvironment.trySelectSingleStorage(storageService)

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

    val id = gridscale.pbs.submit(LocalHost(), description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.pbs.state(LocalHost(), id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.pbs.clean(LocalHost(), id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.pbs.stdOut(LocalHost(), id), gridscale.pbs.stdErr(LocalHost(), id))


  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxLocalOperations))
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)

}
