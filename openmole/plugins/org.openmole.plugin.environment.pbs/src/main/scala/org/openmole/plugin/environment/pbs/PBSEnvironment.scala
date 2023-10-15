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

import _root_.gridscale.effectaside
import org.openmole.core.authentication._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.JavaLogger
import squants._
import squants.information._

object PBSEnvironment extends JavaLogger {

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
    flavour:              gridscale.pbs.PBSFlavour      = Torque,
    name:                 OptionalArgument[String]      = None,
    localSubmission:      Boolean                       = false,
    modules:              Seq[String]                   = Vector(),
    sshProxy:             OptionalArgument[SSHProxy]    = None
  )(implicit authenticationStore: AuthenticationStore, cypher: Cypher, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) = {

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

    EnvironmentProvider { ms ⇒
      import ms._

      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new PBSEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(preference(SSHEnvironment.timeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue),
          sshProxy = sshProxy,
          proxyAuthentication = if (sshProxy.isDefined) Some(SSHAuthentication.find(sshProxy.get.user, sshProxy.get.host, sshProxy.get.port)) else None,
          services = BatchEnvironment.Services(ms)
        )
      }
      else new PBSLocalEnvironment(
        parameters = parameters,
        name = Some(name.getOrElse(varName.value)),
        services = BatchEnvironment.Services(ms)
      )
    }
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
    flavour:              _root_.gridscale.pbs.PBSFlavour,
    modules:              Seq[String])

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: PBSJobService[_, _])(implicit services: BatchEnvironment.Services) =
    submitToCluster(
      environment,
      batchExecutionJob,
      storage,
      space,
      jobService.submit(_, _, _),
      jobService.state(_),
      jobService.delete(_),
      jobService.stdOutErr(_)
    )

}

class PBSEnvironment[Authentication: gridscale.ssh.SSHAuthentication, ProxyAuthentication: gridscale.ssh.SSHAuthentication](
  val user:                String,
  val host:                String,
  val port:                Int,
  val timeout:             Time,
  val parameters:          PBSEnvironment.Parameters,
  val name:                Option[String],
  val authentication:      Authentication,
  val sshProxy:            Option[SSHProxy],
  val proxyAuthentication: Option[ProxyAuthentication],
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState()) { env ⇒
  import services._

  implicit val sshInterpreter: gridscale.effectaside.Effect[gridscale.ssh.SSH] = gridscale.ssh.SSH()
  implicit val systemInterpreter: gridscale.effectaside.Effect[gridscale.effectaside.System] = effectaside.System()
  implicit val localInterpreter: gridscale.effectaside.Effect[gridscale.local.Local] = gridscale.local.Local()

  override def start() = {
    storageService
  }

  override def stop() = {
    state.stopped = true
    cleanSSHStorage(storageService, background = false)
    BatchEnvironment.waitJobKilled(this)
    sshInterpreter().close
  }

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))
  lazy val sshServer = if (sshProxy.isDefined && proxyAuthentication.isDefined) {
    val proxyServer = gridscale.ssh.SSHServer(host = sshProxy.get.host, port = sshProxy.get.port, timeout = timeout)(proxyAuthentication.get)
    gridscale.ssh.SSHServer(host = host, port = port, timeout = timeout, sshProxy = Some(proxyServer))(authentication)
  }
  else gridscale.ssh.SSHServer(host, port, timeout)(authentication)

  lazy val storageService =
    if (parameters.storageSharedLocally) Left {
      val local = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
      (localStorageSpace(local), local)
    }
    else
      Right {
        val ssh =
          sshStorage(
            user = user,
            host = host,
            port = port,
            sshServer = sshServer,
            accessControl = accessControl,
            environment = env,
            sharedDirectory = parameters.sharedDirectory
          )

        (sshStorageSpace(ssh), ssh)
      }

  def execute(batchExecutionJob: BatchExecutionJob) =
    storageService match {
      case Left((space, local)) ⇒ PBSEnvironment.submit(env, batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ PBSEnvironment.submit(env, batchExecutionJob, ssh, space, pbsJobService)
    }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val pbsJobService =
    storageService match {
      case Left((space, local)) ⇒ new PBSJobService(local, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
      case Right((space, ssh))  ⇒ new PBSJobService(ssh, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
    }

}

class PBSLocalEnvironment(
  val parameters:        PBSEnvironment.Parameters,
  val name:              Option[String],
  implicit val services: BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState()) { env ⇒

  import services._

  implicit val localInterpreter: gridscale.effectaside.Effect[gridscale.local.Local] = gridscale.local.Local()
  implicit val systemInterpreter: gridscale.effectaside.Effect[gridscale.effectaside.System] = effectaside.System()

  override def start() = { storage; space }
  override def stop() = {
    state.stopped = true
    HierarchicalStorageSpace.clean(storage, space, background = false)
    BatchEnvironment.waitJobKilled(this)
  }

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
  lazy val space = localStorageSpace(storage)

  def execute(batchExecutionJob: BatchExecutionJob) = PBSEnvironment.submit(env, batchExecutionJob, storage, space, pbsJobService)

  lazy val installRuntime = new RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  import _root_.gridscale.local.LocalHost

  lazy val pbsJobService = new PBSJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHost(), AccessControl(preference(SSHEnvironment.maxConnections)))

}

