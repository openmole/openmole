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

package org.openmole.plugin.environment.condor

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
import org.openmole.plugin.environment.batch.refresh.JobManager
import org.openmole.plugin.environment.batch.storage.StorageInterface
import org.openmole.plugin.environment.gridscale._

object CondorEnvironment {

  def apply(
    user: OptionalArgument[String]                           = None,
    host: OptionalArgument[String]                           = None,
    port: OptionalArgument[Int]                              = 22,
    // TODO not available in the GridScale plugin yet
    //  queue: Option[String] = None,
    openMOLEMemory: OptionalArgument[Information] = None,
    // TODO not available in the GridScale plugin yet
    //wallTime: Option[Duration] = None,
    memory:               OptionalArgument[Information]       = None,
    nodes:                OptionalArgument[Int]               = None,
    coresByNode:          OptionalArgument[Int]               = None,
    sharedDirectory:      OptionalArgument[String]            = None,
    workDirectory:        OptionalArgument[String]            = None,
    requirements:         OptionalArgument[String]            = None,
    timeout:              OptionalArgument[Time]              = None,
    threads:              OptionalArgument[Int]               = None,
    storageSharedLocally: Boolean                             = false,
    localSubmission: Boolean                                  = false,
    name:                 OptionalArgument[String]            = None
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {

    import services._

    val parameters = Parameters(
      openMOLEMemory = openMOLEMemory,
      memory = memory,
      nodes = nodes,
      coresByNode = coresByNode,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      requirements = requirements,
      threads = threads,
      storageSharedLocally = storageSharedLocally)

    EnvironmentProvider { () =>
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new CondorEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue)
        )
      } else {
        new CondorLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value))
        )
      }
    }

  }

  implicit def asSSHServer[A: gridscale.ssh.SSHAuthentication]: AsSSHServer[CondorEnvironment[A]] = new AsSSHServer[CondorEnvironment[A]] {
    override def apply(t: CondorEnvironment[A]) = gridscale.ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  implicit def isJobService[A]: JobServiceInterface[CondorEnvironment[A]] = new JobServiceInterface[CondorEnvironment[A]] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: CondorEnvironment[A], serializedJob: SerializedJob): J = env.submit(serializedJob)
    override def state(env: CondorEnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: CondorEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: CondorEnvironment[A], j: J) = js.stdOutErr(j)
  }

  case class Parameters(
    openMOLEMemory:       Option[Information],
    memory:               Option[Information],
    nodes:                Option[Int],
    coresByNode:          Option[Int],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    requirements:         Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean)

}

class CondorEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     CondorEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment {
  env ⇒

  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def accessControls = List(storageService.accessControl, jobService.accessControl)
    try BatchEnvironment.clean(this, accessControls)
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
    val remoteStorage = StorageInterface.remote(LogicalLinkStorage())
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

    val (remoteScript, workDirectory) = buildScript(serializedJob)

    val description = _root_.gridscale.condor.CondorJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript,
      workDirectory = workDirectory,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coreByNode = parameters.coresByNode orElse parameters.threads,
      requirements = parameters.requirements.map(_root_.gridscale.condor.CondorRequirement.apply)
    )

    gridscale.condor.submit[_root_.gridscale.ssh.SSHServer](env, description)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.condor.state[_root_.gridscale.ssh.SSHServer](env, id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.condor.clean[_root_.gridscale.ssh.SSHServer](env, id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.condor.stdOut[_root_.gridscale.ssh.SSHServer](env, id), gridscale.condor.stdErr[_root_.gridscale.ssh.SSHServer](env, id))

  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxConnections))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)

}




object CondorLocalEnvironment {
  implicit def isJobService: JobServiceInterface[CondorLocalEnvironment] = new JobServiceInterface[CondorLocalEnvironment] {
    override type J = gridscale.cluster.BatchScheduler.BatchJob
    override def submit(env: CondorLocalEnvironment, serializedJob: SerializedJob): J = env.submit(serializedJob)
    override def state(env: CondorLocalEnvironment, j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: CondorLocalEnvironment, j: J): Unit = env.delete(j)
    override def stdOutErr(js: CondorLocalEnvironment, j: J) = js.stdOutErr(j)
  }
}


class CondorLocalEnvironment(
  val parameters:     CondorEnvironment.Parameters,
  val name:           Option[String])(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env =>

  import services._

  lazy val accessControl = AccessControl(services.preference(SSHEnvironment.MaxLocalOperations))
  def accessControls = List(storageService.accessControl, jobService.accessControl)

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def accessControls = List(storageService.accessControl, jobService.accessControl)
    BatchEnvironment.clean(this, accessControls)
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

    val (remoteScript, workDirectory) = buildScript(serializedJob)

    val description = _root_.gridscale.condor.CondorJobDescription(
      executable = "/bin/bash",
      arguments = remoteScript,
      workDirectory = workDirectory,
      memory = Some(BatchEnvironment.requiredMemory(parameters.openMOLEMemory, parameters.memory)),
      nodes = parameters.nodes,
      coreByNode = parameters.coresByNode orElse parameters.threads,
      requirements = parameters.requirements.map(_root_.gridscale.condor.CondorRequirement.apply)
    )

    gridscale.condor.submit(LocalHost(), description)
  }

  def state(id: gridscale.cluster.BatchScheduler.BatchJob) =
    GridScaleJobService.translateStatus(gridscale.condor.state(LocalHost(), id))

  def delete(id: gridscale.cluster.BatchScheduler.BatchJob) =
    gridscale.condor.clean(LocalHost(), id)

  def stdOutErr(id: gridscale.cluster.BatchScheduler.BatchJob) =
    (gridscale.condor.stdOut(LocalHost(), id), gridscale.condor.stdErr(LocalHost(), id))


  lazy val jobService = BatchJobService(env, concurrency = services.preference(SSHEnvironment.MaxLocalOperations))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(jobService, serializedJob)
}

