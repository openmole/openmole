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
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants.{ Time, _ }
import squants.information._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import _root_.gridscale.cluster.*

object SLURMEnvironment:

  def apply(
    user:                 OptionalArgument[String],
    host:                 OptionalArgument[String],
    port:                 OptionalArgument[Int]         = 22,
    partition:            OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    time:                 OptionalArgument[Time]        = None,
    memory:               OptionalArgument[Information] = None,
    qos:                  OptionalArgument[String]      = None,
    gres:                 Seq[Gres]                     = Vector(),
    constraints:          Seq[String]                   = Vector(),
    nodes:                OptionalArgument[Int]         = None,
    nTasks:               OptionalArgument[Int]         = None,
    reservation:          OptionalArgument[String]      = None,
    wckey:                OptionalArgument[String]      = None,
    cpuPerTask:           OptionalArgument[Int]         = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    timeout:              OptionalArgument[Time]        = None,
    reconnect:            OptionalArgument[Time]        = SSHConnection.defaultReconnect,
    storageSharedLocally: Boolean                       = false,
    proxy:                OptionalArgument[SSHProxy]    = None,
    name:                 OptionalArgument[String]      = None,
    localSubmission:      Boolean                       = false,
    forceCopyOnNode:      Boolean                       = false,
    refresh:              OptionalArgument[Time]        = None,
    modules:              OptionalArgument[Seq[String]] = None,
    debug:                Boolean                       = false
  )(using authenticationStore: AuthenticationStore, cypher: Cypher, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) =

    val parameters = Parameters(
      partition = partition,
      openMOLEMemory = openMOLEMemory,
      time = time,
      memory = memory,
      qos = qos,
      gres = gres,
      constraints = constraints,
      nodes = nodes,
      cpuPerTask = cpuPerTask,
      nTasks = nTasks,
      reservation = reservation,
      wckey = wckey,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      forceCopyOnNode = forceCopyOnNode,
      refresh = refresh,
      modules = modules,
      debug = debug)

    EnvironmentBuilder: ms =>
      import ms._

      if !localSubmission
      then
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new SLURMEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(preference(SSHEnvironment.timeOut)),
          reconnect = reconnect,
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue),
          proxy = proxy.map(SSHProxy.authenticated),
          services = BatchEnvironment.Services(ms)
        )
      else
        new SLURMLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          services = BatchEnvironment.Services(ms)
        )


  case class Parameters(
    partition:            Option[String],
    openMOLEMemory:       Option[Information],
    time:                 Option[Time],
    memory:               Option[Information],
    qos:                  Option[String],
    gres:                 Seq[Gres],
    constraints:          Seq[String],
    nodes:                Option[Int],
    cpuPerTask:           Option[Int],
    nTasks:               Option[Int],
    reservation:          Option[String],
    wckey:                Option[String],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean,
    forceCopyOnNode:      Boolean,
    refresh:              Option[Time],
    modules:              Option[Seq[String]],
    debug:                Boolean)

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: SLURMJobService[?], refresh: Option[Time])(using BatchEnvironment.Services, AccessControl.Priority) =
    submitToCluster(
      environment,
      batchExecutionJob,
      storage,
      space,
      jobService.submit,
      jobService.state,
      jobService.delete,
      jobService.stdOutErr,
      refresh)


class SLURMEnvironment(
  val user:              String,
  val host:              String,
  val port:              Int,
  val timeout:           Time,
  val reconnect:         Option[Time],
  val parameters:        SLURMEnvironment.Parameters,
  val name:              Option[String],
  val authentication:    SSHAuthentication,
  val proxy:             Option[SSHProxy.Authenticated],
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState(services)):
  env =>

  import services.*

  implicit lazy val ssh: gridscale.ssh.SSH =
    def proxyValue = proxy.map(p => SSHProxy.toSSHServer(p, timeout))
    val sshServer = gridscale.ssh.SSHServer(host, port, timeout, sshProxy = proxyValue)(authentication)
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

  lazy val sshAccessControl = AccessControl(preference(SSHEnvironment.maxConnections))

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
            accessControl = sshAccessControl,
            environment = env,
            sharedDirectory = parameters.sharedDirectory
          )

        (sshStorageSpace(ssh), ssh)

  lazy val pbsJobService =
    storageService match
      case Left((space, local)) =>
        val installRuntime = RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
        SLURMJobService(local, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, sshAccessControl)
      case Right((space, ssh)) =>
        val installRuntime = RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
        SLURMJobService(ssh, space.tmpDirectory, installRuntime, parameters, HeadNode.ssh, sshAccessControl)

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) =
    storageService match
      case Left((space, local)) => SLURMEnvironment.submit(env, batchExecutionJob, local, space, pbsJobService, parameters.refresh)
      case Right((space, ssh))  => SLURMEnvironment.submit(env, batchExecutionJob, ssh, space, pbsJobService, parameters.refresh)


class SLURMLocalEnvironment(
  val parameters:        SLURMEnvironment.Parameters,
  val name:              Option[String],
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState(services)):
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

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
  lazy val space = localStorageSpace(storage)

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) =
    SLURMEnvironment.submit(env, batchExecutionJob, storage, space, jobService, parameters.refresh)

  lazy val jobService =
    val installRuntime = RuntimeInstallation(Frontend.local, storage, space.baseDirectory)
    new SLURMJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHeadNode(), AccessControl(preference(SSHEnvironment.maxConnections)))


