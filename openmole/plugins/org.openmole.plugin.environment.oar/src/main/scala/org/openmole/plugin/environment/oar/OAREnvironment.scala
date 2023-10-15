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

import _root_.gridscale.effectaside
import org.openmole.core.authentication._
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
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
    modules:              Seq[String]                   = Vector(),
    sshProxy:             OptionalArgument[SSHProxy]    = None
  )(implicit authenticationStore: AuthenticationStore, cypher: Cypher, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) = {

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

    EnvironmentProvider { ms ⇒
      import ms._

      if (!localSubmission) {
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
          sshProxy = sshProxy,
          proxyAuthentication = if (sshProxy.isDefined) Some(SSHAuthentication.find(sshProxy.get.user, sshProxy.get.host, sshProxy.get.port)) else None,
          services = BatchEnvironment.Services(ms)
        )
      }
      else
        new OARLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          services = BatchEnvironment.Services(ms)
        )
    }
  }

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

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: OARJobService[_, _])(implicit services: BatchEnvironment.Services) =
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

class OAREnvironment[Authentication: gridscale.ssh.SSHAuthentication, ProxyAuthentication: gridscale.ssh.SSHAuthentication](
  val parameters:          OAREnvironment.Parameters,
  val user:                String,
  val host:                String,
  val port:                Int,
  val timeout:             Time,
  val name:                Option[String],
  val authentication:      Authentication,
  val sshProxy:            Option[SSHProxy],
  val proxyAuthentication: Option[ProxyAuthentication],
  implicit val services: BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState()) { env ⇒

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
      case Left((space, local)) ⇒ OAREnvironment.submit(env, batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ OAREnvironment.submit(env, batchExecutionJob, ssh, space, pbsJobService)
    }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val pbsJobService =
    storageService match {
      case Left((space, local)) ⇒ new OARJobService(local, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
      case Right((space, ssh))  ⇒ new OARJobService(ssh, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
    }

}

class OARLocalEnvironment(
  val parameters:        OAREnvironment.Parameters,
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

  def execute(batchExecutionJob: BatchExecutionJob) = OAREnvironment.submit(env, batchExecutionJob, storage, space, jobService)

  lazy val installRuntime = new RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  import _root_.gridscale.local.LocalHost

  lazy val jobService = new OARJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHost(), AccessControl(preference(SSHEnvironment.maxConnections)))

}
