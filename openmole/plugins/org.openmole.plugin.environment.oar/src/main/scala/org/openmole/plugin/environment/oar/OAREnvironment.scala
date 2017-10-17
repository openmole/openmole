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
import org.openmole.plugin.environment.batch.control.{ LimitedAccess, UnlimitedAccess }
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._
import freedsl.dsl._
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

    if(!localSubmission) {
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
        authentication = SSHAuthentication.find(userValue, hostValue, portValue).apply
      )
    } else
      new OARLocalEnvironment(
        parameters = parameters,
        name = Some(name.getOrElse(varName.value))
      )
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
    val queue:                Option[String],
    val core:                 Option[Int],
    val cpu:                  Option[Int],
    val wallTime:             Option[Time],
    val openMOLEMemory:       Option[Information],
    val sharedDirectory:      Option[String],
    val workDirectory:        Option[String],
    val threads:              Option[Int],
    val storageSharedLocally: Boolean,
    val bestEffort:           Boolean
  )

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

  lazy val usageControl =
    new LimitedAccess(
      services.preference(SSHEnvironment.MaxConnections),
      services.preference(SSHEnvironment.MaxOperationsByMinute)
    )

  implicit val sshInterpreter = gridscale.ssh.SSHInterpreter()
  implicit val systemInterpreter = freedsl.system.SystemInterpreter()
  implicit val errorHandler = freedsl.errorhandler.ErrorHandlerInterpreter()

  override def stop() = {
    try super.stop()
    finally sshInterpreter.close()
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
      usageControl = usageControl,
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
    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = parameters.core,
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    val id = gridscale.oar.submit[DSL, _root_.gridscale.ssh.SSHServer](env, description).eval
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.oar.state[DSL, _root_.gridscale.ssh.SSHServer](env, id).eval)

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.oar.clean[DSL, _root_.gridscale.ssh.SSHServer](env, id).eval

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) = {
    val op =
      for {
        o ← gridscale.oar.stdOut[DSL, _root_.gridscale.ssh.SSHServer](env, id)
        e ← gridscale.oar.stdErr[DSL, _root_.gridscale.ssh.SSHServer](env, id)
      } yield (o, e)
    op.eval
  }

  lazy val jobService = new BatchJobService(env, usageControl)
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)
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

  lazy val usageControl = UnlimitedAccess

  implicit val localInterpreter = gridscale.local.LocalInterpreter()
  implicit val systemInterpreter = freedsl.system.SystemInterpreter()
  implicit val errorHandler = freedsl.errorhandler.ErrorHandlerInterpreter()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  lazy val storageService =
   localStorageService(
      environment = env,
      usageControl = usageControl,
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
    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = parameters.queue,
      cpu = parameters.cpu,
      core = parameters.core,
      wallTime = parameters.wallTime,
      bestEffort = parameters.bestEffort
    )

    val id = gridscale.oar.submit[DSL, LocalHost](LocalHost(), description).eval
    BatchJob(id, result)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.oar.state[DSL, LocalHost](LocalHost(), id).eval)

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.oar.clean[DSL, LocalHost](LocalHost(), id).eval

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) = {
    val op =
      for {
        o ← gridscale.oar.stdOut[DSL, LocalHost](LocalHost(), id)
        e ← gridscale.oar.stdErr[DSL, LocalHost](LocalHost(), id)
      } yield (o, e)
    op.eval
  }

  lazy val jobService = new BatchJobService(env, usageControl)
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)
}
