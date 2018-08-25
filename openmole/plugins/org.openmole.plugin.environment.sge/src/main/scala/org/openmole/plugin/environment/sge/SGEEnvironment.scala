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
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants.Time
import squants.information._

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
    localSubmission:      Boolean                       = false)(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
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

    EnvironmentProvider { () ⇒
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
      }
      else
        new SGELocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value))
        )
    }
  }

  case class Parameters(
    queue:                Option[String],
    openMOLEMemory:       Option[Information],
    wallTime:             Option[Time],
    memory:               Option[Information],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean)

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: SGEJobService[_, _])(implicit services: BatchEnvironment.Services) =
    submitToCluster(
      batchExecutionJob,
      storage,
      space,
      jobService.submit(_, _),
      jobService.state(_),
      jobService.delete(_),
      jobService.stdOutErr(_)
    )

}

class SGEEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     SGEEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()
  implicit val localInterpreter = gridscale.local.Local()

  override def start() = {
    storageService
  }

  override def stop() = {
    storageService match {
      case Left((space, local)) ⇒ HierarchicalStorageSpace.clean(local, space)
      case Right((space, ssh))  ⇒ HierarchicalStorageSpace.clean(ssh, space)
    }
    sshInterpreter().close
  }

  lazy val accessControl = AccessControl(preference(SSHEnvironment.MaxConnections))
  lazy val sshServer = gridscale.ssh.SSHServer(host, port, timeout)(authentication)

  lazy val storageService =
    if (parameters.storageSharedLocally) Left {
      val local = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.MaxConnections)))
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
      case Left((space, local)) ⇒ SGEEnvironment.submit(batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ SGEEnvironment.submit(batchExecutionJob, ssh, space, pbsJobService)
    }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val pbsJobService =
    storageService match {
      case Left((space, local)) ⇒ new SGEJobService(local, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
      case Right((space, ssh))  ⇒ new SGEJobService(ssh, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
    }

}

class SGELocalEnvironment(
  val parameters: SGEEnvironment.Parameters,
  val name:       Option[String])(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  override def start() = { storage; space }
  override def stop() = { HierarchicalStorageSpace.clean(storage, space) }

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.MaxConnections)))
  lazy val space = localStorageSpace(storage)

  def execute(batchExecutionJob: BatchExecutionJob) = SGEEnvironment.submit(batchExecutionJob, storage, space, jobService)

  lazy val installRuntime = new RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  import _root_.gridscale.local.LocalHost

  lazy val jobService = new SGEJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHost(), AccessControl(preference(SSHEnvironment.MaxConnections)))

}
