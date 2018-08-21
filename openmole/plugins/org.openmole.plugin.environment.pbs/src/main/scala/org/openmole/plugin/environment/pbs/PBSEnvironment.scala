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
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.gridscale.LogicalLinkStorage
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
    localSubmission:      Boolean                       = false
  )(implicit services: BatchEnvironment.Services, authenticationStore: AuthenticationStore, cypher: Cypher, varName: sourcecode.Name) = {
    import services._

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
      flavour = flavour
    )

    EnvironmentProvider { () ⇒
      if (!localSubmission) {
        val userValue = user.mustBeDefined("user")
        val hostValue = host.mustBeDefined("host")
        val portValue = port.mustBeDefined("port")

        new PBSEnvironment(
          user = userValue,
          host = hostValue,
          port = portValue,
          timeout = timeout.getOrElse(services.preference(SSHEnvironment.TimeOut)),
          parameters = parameters,
          name = Some(name.getOrElse(varName.value)),
          authentication = SSHAuthentication.find(userValue, hostValue, portValue)
        )
      }
      else new PBSLocalEnvironment(
        parameters = parameters,
        name = Some(name.getOrElse(varName.value))
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
    flavour:              _root_.gridscale.pbs.PBSFlavour)
}

class PBSEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:           String,
  val host:           String,
  val port:           Int,
  val timeout:        Time,
  val parameters:     PBSEnvironment.Parameters,
  val name:           Option[String],
  val authentication: A
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment {
  env ⇒
  import services._

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = effectaside.System()
  implicit val localInterpreter = gridscale.local.Local()

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def accessControls = List(getaccessControl(storageService), pbsJobService.accessControl)
    try BatchEnvironment.clean(this, accessControls)
    finally sshInterpreter().close
  }

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

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

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage())

    storageService match {
      case Left((space, local)) ⇒
        BatchEnvironment.serializeJob(local, remoteStorage, batchExecutionJob, StorageSpace.createJobDirectory(local, space), space.replicaDirectory, StorageSpace.backgroundRm(local, _, true))
      case Right((space, ssh)) ⇒ BatchEnvironment.serializeJob(ssh, remoteStorage, batchExecutionJob, StorageSpace.createJobDirectory(ssh, space), space.replicaDirectory, StorageSpace.backgroundRm(ssh, _, true))
    }
  }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val pbsJobService =
    storageService match {
      case Left((_, local)) ⇒ new PBSJobService(local, installRuntime, parameters, sshServer, accessControl)
      case Right((_, ssh))  ⇒ new PBSJobService(ssh, installRuntime, parameters, sshServer, accessControl)
    }

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(pbsJobService, serializedJob)
}

class PBSLocalEnvironment(
  val parameters: PBSEnvironment.Parameters,
  val name:       Option[String]
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._

  implicit val localInterpreter = gridscale.local.Local()

  import env.services.preference
  import org.openmole.plugin.environment.ssh._

  override def start() = BatchEnvironment.start(this)

  override def stop() = {
    def accessControls = List(storage.accessControl, pbsJobService.accessControl)
    BatchEnvironment.clean(this, accessControls)
  }

  lazy val storage = localStorage(env, parameters.sharedDirectory, AccessControl(preference(SSHEnvironment.MaxConnections)))
  lazy val space = localStorageSpace(storage)

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage())
    BatchEnvironment.serializeJob(storage, remoteStorage, batchExecutionJob, StorageSpace.createJobDirectory(storage, space), space.replicaDirectory, StorageSpace.backgroundRm(storage, _, true))
  }

  lazy val installRuntime = new RuntimeInstallation(Frontend.local, storage, space.baseDirectory)

  import _root_.gridscale.local.LocalHost

  lazy val pbsJobService = new PBSJobService(storage, installRuntime, parameters, LocalHost(), AccessControl(preference(SSHEnvironment.MaxConnections)))

  override def submitSerializedJob(serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(pbsJobService, serializedJob)

}

