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

package org.openmole.plugin.environment.sge

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
import org.openmole.plugin.environment.gridscale._

object SGEEnvironment {
  def apply(
    user:                 OptionalArgument[String]      = None,
    host:                 OptionalArgument[String]      = None,
    port:                 OptionalArgument[Int]         = 22,
    queue:                OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    wallTime:             OptionalArgument[Time]        = None,
    memory:               OptionalArgument[Information] = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    timeout:              OptionalArgument[Time]        = None,
    name:                 OptionalArgument[String]      = None,
    localSubmission: Boolean                            = false)(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._


    val parameters = Parameters(
      queue = queue,
      openMOLEMemory = openMOLEMemory,
      wallTime = wallTime,
      memory = memory,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally)

    EnvironmentProvider { () =>
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new SGEEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue)
        )
      } else
        new SGELocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value))
        )
    }
  }

  implicit def asSSHServer[A: gridscale.ssh.SSHAuthentication]: AsSSHServer[SGEEnvironment[A]] = new AsSSHServer[SGEEnvironment[A]] {
    override def apply(t: SGEEnvironment[A]) = gridscale.ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  implicit def isJobService[A]: JobServiceInterface[SGEEnvironment[A]] = new JobServiceInterface[SGEEnvironment[A]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: SGEEnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: SGEEnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SGEEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: SGEEnvironment[A], j: J) = js.stdOutErr(j)
  }

  case class Parameters(
    queue:                   Option[String],
    openMOLEMemory: Option[Information],
    wallTime:                Option[Time],
    memory:                  Option[Information],
    sharedDirectory:         Option[String],
    workDirectory:           Option[String],
    threads:        Option[Int],
    storageSharedLocally:    Boolean)

}

class SGEEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     SGEEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env =>

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

    val description = _root_.gridscale.sge.SGEJobDescription(
      command = s"/bin/bash $remoteScript",
      queue = parameters.queue,
      workDirectory = workDirectory,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
    )

    val id = gridscale.sge.submit[_root_.gridscale.ssh.SSHServer](env, description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.sge.state[_root_.gridscale.ssh.SSHServer](env, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.sge.clean[_root_.gridscale.ssh.SSHServer](env, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.sge.stdOut[_root_.gridscale.ssh.SSHServer](env, id), gridscale.sge.stdErr[_root_.gridscale.ssh.SSHServer](env, id))

  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxConnections))
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)
}


object SGELocalEnvironment {
  implicit def isJobService: JobServiceInterface[SGELocalEnvironment] = new JobServiceInterface[SGELocalEnvironment] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: SGELocalEnvironment, serializedJob: SerializedJob): BatchJob[J] = env.submit(serializedJob)
    override def state(env: SGELocalEnvironment, j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SGELocalEnvironment, j: J): Unit = env.delete(j)
    override def stdOutErr(js: SGELocalEnvironment, j: J) = js.stdOutErr(j)
  }
}


class SGELocalEnvironment(
  val parameters:     SGEEnvironment.Parameters,
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
    val description = _root_.gridscale.sge.SGEJobDescription(
      command = s"/bin/bash $remoteScript",
      queue = parameters.queue,
      workDirectory = workDirectory,
      wallTime = parameters.wallTime,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
    )
    val id = gridscale.sge.submit(LocalHost(), description)
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.sge.state(LocalHost(), id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.sge.clean(LocalHost(), id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.sge.stdOut(LocalHost(), id), gridscale.sge.stdErr(LocalHost(), id))


  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxLocalOperations))
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)

}
