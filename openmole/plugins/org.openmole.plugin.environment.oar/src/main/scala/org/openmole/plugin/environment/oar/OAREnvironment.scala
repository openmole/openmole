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
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import squants._
import squants.information._

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
    localSubmission:      Boolean                       = false,
    modules: Seq[String] = Vector(),
  )(implicit authenticationStore: AuthenticationStore, cypher: Cypher, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) =

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
      bestEffort = bestEffort,
      modules = modules
    )

    EnvironmentProvider: (ms, cache) ⇒
      import ms._

      if !localSubmission
      then
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new OAREnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(preference(SSHEnvironment.timeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue),
          services = BatchEnvironment.Services(ms, cache)
        )
      else
        new OARLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          services = BatchEnvironment.Services(ms, cache)
        )


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
    bestEffort:           Boolean,
    modules:              Seq[String])

  def nbCores(parameters: Parameters) = parameters.core orElse parameters.threads

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: OARJobService[?])(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    submitToCluster(
      environment,
      batchExecutionJob,
      storage,
      space,
      jobService.submit,
      jobService.state,
      jobService.delete,
      jobService.stdOutErr
    )

}

class OAREnvironment[A: gridscale.ssh.SSHAuthentication](
  val parameters:        OAREnvironment.Parameters,
  val user:              String,
  val host:              String,
  val port:              Int,
  val timeout:           Time,
  val name:              Option[String],
  val authentication:    A,
  implicit val services: BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState(services)) { env ⇒

  import services._

  implicit lazy val ssh: gridscale.ssh.SSH =
    val sshServer = gridscale.ssh.SSHServer(host, port, timeout)(authentication)
    gridscale.ssh.SSH(sshServer)

  override def start() =
    storageService
    AccessControl.defaultPrirority:
      cleanSSHStorage(storageService, background = true)

  override def stop() =
    state.stopped = true
    AccessControl.defaultPrirority:
      cleanSSHStorage(storageService, background = false)
    BatchEnvironment.waitJobKilled(this)
    ssh.close

  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))
  
  lazy val storageService =
    if parameters.storageSharedLocally
    then Left:
      val local = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
      (localStorageSpace(local), local)
    else
      Right:
        val ssh =
          sshStorage(
            user = user,
            host = host,
            port = port,
            accessControl = accessControl,
            environment = env,
            sharedDirectory = parameters.sharedDirectory
          )

        (sshStorageSpace(ssh), ssh)

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) =
    storageService match
      case Left((space, local)) ⇒ OAREnvironment.submit(env, batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ OAREnvironment.submit(env, batchExecutionJob, ssh, space, pbsJobService)

  lazy val installRuntime =
    storageService match
      case Left((space, local)) ⇒ RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)

  lazy val pbsJobService =
    import _root_.gridscale.cluster.HeadNode
    storageService match
      case Left((space, local)) ⇒ OARJobService(local, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, accessControl)
      case Right((space, ssh))  ⇒ OARJobService(ssh, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, accessControl)

}

class OARLocalEnvironment(
  val parameters:        OAREnvironment.Parameters,
  val name:              Option[String],
  implicit val services: BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState(services)):
  env =>

  import services._

  override def start() =
    storage
    space
    AccessControl.defaultPrirority:
      HierarchicalStorageSpace.clean(storage, space, background = true)

  override def stop() =
    state.stopped = true
    AccessControl.defaultPrirority:
      HierarchicalStorageSpace.clean(storage, space, background = false)
    BatchEnvironment.waitJobKilled(this)

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
  lazy val space = localStorageSpace(storage)

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) = OAREnvironment.submit(env, batchExecutionJob, storage, space, jobService)

  lazy val installRuntime = RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  lazy val jobService = OARJobService(storage, space.tmpDirectory, installRuntime, parameters, _root_.gridscale.cluster.LocalHeadNode(), AccessControl(preference(SSHEnvironment.maxConnections)))

