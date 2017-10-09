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

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.control.LimitedAccess
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants._
import squants.information._
import freedsl.dsl._
import org.openmole.plugin.environment.gridscale.GridScaleJobService

object OAREnvironment {

  def apply(
    user:                 String,
    host:                 String,
    port:                 Int                           = 22,
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
    bestEffort:           Boolean                       = true
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._
    new OAREnvironment(
      user = user,
      host = host,
      port = port,
      queue = queue,
      core = core,
      cpu = cpu,
      wallTime = wallTime,
      openMOLEMemory = openMOLEMemory,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      name = Some(name.getOrElse(varName.value)),
      bestEffort = bestEffort,
      authentication = SSHAuthentication.find(user, host, port).apply
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

}

class OAREnvironment[A: gridscale.ssh.SSHAuthentication](
    val user:                    String,
    val host:                    String,
    val port:                    Int,
    val queue:                   Option[String],
    val core:                    Option[Int],
    val cpu:                     Option[Int],
    val wallTime:                Option[Time],
    override val openMOLEMemory: Option[Information],
    val sharedDirectory:         Option[String],
    val workDirectory:           Option[String],
    override val threads:        Option[Int],
    val storageSharedLocally:    Boolean,
    override val name:           Option[String],
    val bestEffort:              Boolean,
    val authentication:          A
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  lazy val usageControl =
    new LimitedAccess(
      services.preference(SSHEnvironment.MaxConnections),
      services.preference(SSHEnvironment.MaxOperationsByMinute)
    )

  def timeout = services.preference(SSHEnvironment.TimeOut)
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
      sharedDirectory = sharedDirectory,
      storageSharedLocally = storageSharedLocally
    )

  override def trySelectStorage(files: ⇒ Vector[File]) = BatchEnvironment.trySelectSingleStorage(storageService)

  val installRuntime = new RuntimeInstallation(
    host = host,
    port = port,
    timeout = timeout,
    storageService = storageService,
    authentication = authentication
  )

  def submit(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      import services._
      SharedStorage.buildScript(
        env.installRuntime.apply,
        env.workDirectory,
        env.openMOLEMemory,
        env.threads,
        serializedJob,
        env.storageService
      )
    }

    val (remoteScript, result, workDirectory) = buildScript(serializedJob)
    val description = gridscale.oar.OARJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory,
      queue = env.queue,
      cpu = env.cpu,
      core = env.core,
      wallTime = env.wallTime,
      bestEffort = env.bestEffort
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
