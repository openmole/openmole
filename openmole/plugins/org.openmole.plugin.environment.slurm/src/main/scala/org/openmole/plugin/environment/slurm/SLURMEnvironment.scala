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
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.ssh._
import org.openmole.tool.crypto.Cypher
import squants.{ Time, _ }
import squants.information._
import org.openmole.plugin.environment.batch.storage._

object SLURMEnvironment {

  def apply(
    user:                 OptionalArgument[String]      = None,
    host:                 OptionalArgument[String]      = None,
    port:                 OptionalArgument[Int]         = 22,
    queue:                OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    wallTime:             OptionalArgument[Time]        = None,
    memory:               OptionalArgument[Information] = None,
    qos:                  OptionalArgument[String]      = None,
    gres:                 Seq[Gres]                     = List(),
    constraints:          Seq[String]                   = List(),
    nodes:                OptionalArgument[Int]         = None,
    nTasks:               OptionalArgument[Int]         = None,
    coresByNode:          OptionalArgument[Int]         = None,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    threads:              OptionalArgument[Int]         = None,
    timeout:              OptionalArgument[Time]        = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None,
    localSubmission:      Boolean                       = false,
    forceCopyOnNode:      Boolean                       = false,
    debug:                Boolean                       = false
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._

    val parameters = Parameters(
      queue = queue,
      openMOLEMemory = openMOLEMemory,
      wallTime = wallTime,
      memory = memory,
      qos = qos,
      gres = gres,
      constraints = constraints,
      nodes = nodes,
      coresByNode = coresByNode,
      nTasks = nTasks,
      sharedDirectory = sharedDirectory,
      workDirectory = workDirectory,
      threads = threads,
      storageSharedLocally = storageSharedLocally,
      forceCopyOnNode = forceCopyOnNode,
      debug = debug)

    EnvironmentProvider { ms ⇒
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new SLURMEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.timeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue),
          services = services.set(ms)
        )
      }
      else
        new SLURMLocalEnvironment(
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          services = services.set(ms)
        )
    }

  }

  case class Parameters(
    queue:                Option[String],
    openMOLEMemory:       Option[Information],
    wallTime:             Option[Time],
    memory:               Option[Information],
    qos:                  Option[String],
    gres:                 Seq[Gres],
    constraints:          Seq[String],
    nodes:                Option[Int],
    coresByNode:          Option[Int],
    nTasks:               Option[Int],
    sharedDirectory:      Option[String],
    workDirectory:        Option[String],
    threads:              Option[Int],
    storageSharedLocally: Boolean,
    forceCopyOnNode:      Boolean,
    debug:                Boolean)

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: SLURMJobService[_, _])(implicit services: BatchEnvironment.Services) =
    submitToCluster(
      batchExecutionJob,
      storage,
      space,
      jobService.submit(_, _, _),
      jobService.state(_),
      jobService.delete(_),
      jobService.stdOutErr(_))

}

class SLURMEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:              String,
  val host:              String,
  val port:              Int,
  val timeout:           Time,
  val parameters:        SLURMEnvironment.Parameters,
  val name:              Option[String],
  val authentication:    A,
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment {
  env ⇒

  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()
  implicit val localInterpreter = gridscale.local.Local()

  override def start() = { storageService }

  override def stop() = {
    stopped = true
    cleanSSHStorage(storageService, background = false)
    BatchEnvironment.waitJobKilled(this)
    sshInterpreter().close
  }

  import env.services.{ threadProvider, preference }
  import org.openmole.plugin.environment.ssh._

  lazy val sshServer = gridscale.ssh.SSHServer(host, port, timeout)(authentication)
  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))

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
      case Left((space, local)) ⇒ SLURMEnvironment.submit(batchExecutionJob, local, space, pbsJobService)
      case Right((space, ssh))  ⇒ SLURMEnvironment.submit(batchExecutionJob, ssh, space, pbsJobService)
    }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val pbsJobService =
    storageService match {
      case Left((space, local)) ⇒ new SLURMJobService(local, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
      case Right((space, ssh))  ⇒ new SLURMJobService(ssh, space.tmpDirectory, installRuntime, parameters, sshServer, accessControl)
    }

}

class SLURMLocalEnvironment(
  val parameters:        SLURMEnvironment.Parameters,
  val name:              Option[String],
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val localInterpreter = gridscale.local.Local()
  implicit val systemInterpreter = effectaside.System()

  override def start() = { storage; space; HierarchicalStorageSpace.clean(storage, space, background = true) }
  override def stop() = {
    stopped = true
    HierarchicalStorageSpace.clean(storage, space, background = false)
    BatchEnvironment.waitJobKilled(this)
  }

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
  lazy val space = localStorageSpace(storage)

  def execute(batchExecutionJob: BatchExecutionJob) = SLURMEnvironment.submit(batchExecutionJob, storage, space, jobService)

  lazy val installRuntime = new RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  import _root_.gridscale.local.LocalHost

  lazy val jobService = new SLURMJobService(storage, space.tmpDirectory, installRuntime, parameters, LocalHost(), AccessControl(preference(SSHEnvironment.maxConnections)))

}

