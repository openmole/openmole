/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.environment.ssh

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

import effectaside._
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.ConfigurationLocation
import org.openmole.core.threadprovider.{ IUpdatable, Updater }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.plugin.environment.gridscale.LogicalLinkStorage
import org.openmole.tool.crypto._
import org.openmole.tool.lock._
import org.openmole.tool.logger.JavaLogger
import squants.information._
import squants.time.TimeConversions._

import scala.ref.WeakReference

object SSHEnvironment extends JavaLogger {

  val MaxLocalOperations = ConfigurationLocation("ClusterEnvironment", "MaxLocalOperations", Some(100))
  val MaxConnections = ConfigurationLocation("SSHEnvironment", "MaxConnections", Some(5))

  val UpdateInterval = ConfigurationLocation("SSHEnvironment", "UpdateInterval", Some(10 seconds))
  val TimeOut = ConfigurationLocation("SSHEnvironment", "Timeout", Some(1 minutes))

  def apply(
    user:                 String,
    host:                 String,
    slots:                Int,
    port:                 Int                           = 22,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    threads:              OptionalArgument[Int]         = None,
    storageSharedLocally: Boolean                       = false,
    name:                 OptionalArgument[String]      = None
  )(implicit services: BatchEnvironment.Services, cypher: Cypher, authenticationStore: AuthenticationStore, varName: sourcecode.Name) = {
    import services._

    EnvironmentProvider { () ⇒
      new SSHEnvironment(
        user = user,
        host = host,
        slots = slots,
        port = port,
        sharedDirectory = sharedDirectory,
        workDirectory = workDirectory,
        openMOLEMemory = openMOLEMemory,
        threads = threads,
        storageSharedLocally = storageSharedLocally,
        name = Some(name.getOrElse(varName.value)),
        authentication = SSHAuthentication.find(user, host, port)
      )
    }
  }

  case class SSHJob(id: Long) extends AnyVal

  sealed trait SSHRunState
  case class Queued(description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) extends SSHRunState
  case class Submitted(pid: gridscale.ssh.JobId) extends SSHRunState
  case object Failed extends SSHRunState

}

class SSHEnvironment[A: gridscale.ssh.SSHAuthentication](
  val user:                 String,
  val host:                 String,
  val slots:                Int,
  val port:                 Int,
  val sharedDirectory:      Option[String],
  val workDirectory:        Option[String],
  val openMOLEMemory:       Option[Information],
  val threads:              Option[Int],
  val storageSharedLocally: Boolean,
  val name:                 Option[String],
  val authentication:       A
)(implicit val services: BatchEnvironment.Services) extends BatchEnvironment { env ⇒

  import services._
  override def updateInterval = UpdateInterval.fixed(env.services.preference(SSHEnvironment.UpdateInterval))

  lazy val jobUpdater = new SSHJobService.Updater(WeakReference(this))

  val queuesLock = new ReentrantLock()
  val jobsStates = collection.mutable.TreeMap[SSHEnvironment.SSHJob, SSHEnvironment.SSHRunState]()(Ordering.by(_.id))
  val jobId = new AtomicLong()

  type PID = Int
  val submittedJobs = collection.mutable.Map[SSHEnvironment.SSHJob, PID]()

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = System()
  implicit val localInterpreter = gridscale.local.Local()

  def timeout = services.preference(SSHEnvironment.TimeOut)

  override def start() = {
    storageService
    import services.threadProvider
    Updater.delay(jobUpdater, services.preference(SSHEnvironment.UpdateInterval))
  }

  override def stop() = {
    storageService match {
      case Left((space, local)) ⇒ HierarchicalStorageSpace.clean(local, space)
      case Right((space, ssh))  ⇒ HierarchicalStorageSpace.clean(ssh, space)
    }
    jobUpdater.stop = true
    sshInterpreter().close
  }

  import env.services.preference

  lazy val accessControl = AccessControl(preference(SSHEnvironment.MaxConnections))

  lazy val sshServer = gridscale.ssh.SSHServer(env.host, env.port, env.timeout)(env.authentication)

  lazy val storageService =
    if (storageSharedLocally) Left {
      val local = localStorage(env, sharedDirectory, AccessControl(preference(SSHEnvironment.MaxConnections)))
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
            sharedDirectory = sharedDirectory
          )

        (sshStorageSpace(ssh), ssh)
      }

  override def serializeJob(batchExecutionJob: BatchExecutionJob) = {
    val remoteStorage = LogicalLinkStorage.remote(LogicalLinkStorage())

    storageService match {
      case Left((space, local)) ⇒ BatchEnvironment.serializeJob(local, remoteStorage, batchExecutionJob, HierarchicalStorageSpace.createJobDirectory(local, space), space.replicaDirectory, HierarchicalStorageSpace.backgroundRm(local, _, true))
      case Right((space, ssh))  ⇒ BatchEnvironment.serializeJob(ssh, remoteStorage, batchExecutionJob, HierarchicalStorageSpace.createJobDirectory(ssh, space), space.replicaDirectory, HierarchicalStorageSpace.backgroundRm(ssh, _, true))
    }
  }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val sshJobService =
    storageService match {
      case Left((space, local)) ⇒ new SSHJobService(local, services, installRuntime, env, accessControl)
      case Right((space, ssh))  ⇒ new SSHJobService(ssh, services, installRuntime, env, accessControl)
    }

  override def submitSerializedJob(batchExecutionJob: BatchExecutionJob, serializedJob: SerializedJob) =
    BatchEnvironment.submitSerializedJob(sshJobService, batchExecutionJob, serializedJob)
}

