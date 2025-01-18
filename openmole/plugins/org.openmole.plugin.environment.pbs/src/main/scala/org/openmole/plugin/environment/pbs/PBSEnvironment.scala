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
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.JavaLogger
import squants._
import squants.information._

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

object PBSEnvironment extends JavaLogger:

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
    reconnect:            OptionalArgument[Time]        = SSHConnection.defaultReconnect,
    flavour:              gridscale.pbs.PBSFlavour      = Torque,
    name:                 OptionalArgument[String]      = None,
    localSubmission:      Boolean                       = false,
    modules: Seq[String] = Vector(),
  )(implicit authenticationStore: AuthenticationStore, cypher: Cypher, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) =

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
      flavour = flavour,
      modules = modules
    )

    EnvironmentBuilder: (ms, cache) ⇒
      import ms._

      if !localSubmission
      then
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new PBSEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(preference(SSHEnvironment.timeOut)),
          reconnect = reconnect,
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue),
          services = BatchEnvironment.Services(ms, cache)
        )
      else new PBSLocalEnvironment(
        parameters = parameters,
        name = Some(name.getOrElse(varName.value)),
        services = BatchEnvironment.Services(ms, cache)
      )


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
    flavour:              _root_.gridscale.pbs.PBSFlavour,
    modules:              Seq[String])

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: PBSJobService[?])(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
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


class PBSEnvironment(
  val user:              String,
  val host:              String,
  val port:              Int,
  val timeout:           Time,
  val reconnect:         Option[Time],
  val parameters:        PBSEnvironment.Parameters,
  val name:              Option[String],
  val authentication:    SSHAuthentication,
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState(services)):
  env =>

  import services.*

  implicit lazy val ssh: gridscale.ssh.SSH =
    val sshServer = gridscale.ssh.SSHServer(host, port, timeout)(authentication)
    gridscale.ssh.SSH(sshServer, reconnect = reconnect)

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

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))

  lazy val storageService =
    if parameters.storageSharedLocally
    then
      Left:
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
      case Left((space, local)) ⇒ PBSEnvironment.submit(env, batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ PBSEnvironment.submit(env, batchExecutionJob, ssh, space, pbsJobService)

  def frontend = Frontend.ssh(host, port, timeout, authentication)

  lazy val installRuntime =
    storageService match 
      case Left((space, local)) ⇒ RuntimeInstallation(frontend, local, space.baseDirectory)
      case Right((space, ssh))  ⇒ RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)

  lazy val pbsJobService =
    import _root_.gridscale.cluster.HeadNode
    storageService match 
      case Left((space, local)) ⇒ PBSJobService(local, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, accessControl)
      case Right((space, ssh))  ⇒ PBSJobService(ssh, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, accessControl)



class PBSLocalEnvironment(
  val parameters:        PBSEnvironment.Parameters,
  val name:              Option[String],
  implicit val services: BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState(services)):
  env =>

  import services.*

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

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) = PBSEnvironment.submit(env, batchExecutionJob, storage, space, pbsJobService)

  lazy val installRuntime = RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  lazy val pbsJobService =
    import _root_.gridscale.cluster.LocalHeadNode
    new PBSJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHeadNode(), AccessControl(preference(SSHEnvironment.maxConnections)))

