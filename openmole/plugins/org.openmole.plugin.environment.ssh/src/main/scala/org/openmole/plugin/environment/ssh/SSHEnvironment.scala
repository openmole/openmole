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

import gridscale.effectaside._
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.ConfigurationLocation
import org.openmole.core.threadprovider.Updater
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.crypto._
import org.openmole.tool.lock._
import org.openmole.tool.logger.JavaLogger
import squants.information._
import squants.time.TimeConversions._

import scala.ref.WeakReference

object SSHEnvironment extends JavaLogger {

  val maxLocalOperations = ConfigurationLocation("ClusterEnvironment", "MaxLocalOperations", Some(100))
  val maxConnections = ConfigurationLocation("SSHEnvironment", "MaxConnections", Some(5))

  val updateInterval = ConfigurationLocation("SSHEnvironment", "UpdateInterval", Some(10 seconds))
  val timeOut = ConfigurationLocation("SSHEnvironment", "Timeout", Some(1 minutes))

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

    EnvironmentProvider { ms ⇒
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
        authentication = SSHAuthentication.find(user, host, port),
        services = services.set(ms)
      )
    }
  }

  case class SSHJob(id: Long) extends AnyVal

  sealed trait SSHRunState
  case class Queued(description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) extends SSHRunState
  case class Submitted(pid: gridscale.ssh.JobId) extends SSHRunState
  case object Failed extends SSHRunState

  class SSHJobStateRegistry {
    val jobsStates = collection.mutable.TreeMap[SSHEnvironment.SSHJob, SSHEnvironment.SSHRunState]()(Ordering.by(_.id))
    val queuesLock = new ReentrantLock()
    val jobId = new AtomicLong()

    def registerJob(description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) = queuesLock {
      val job = SSHEnvironment.SSHJob(jobId.getAndIncrement())
      jobsStates.put(job, SSHEnvironment.Queued(description, batchExecutionJob))
      job
    }

    def update(job: SSHJob, state: SSHRunState) = queuesLock { jobsStates.put(job, state) }
    def get(job: SSHJob) = queuesLock { jobsStates.get(job) }
    def remove(job: SSHJob) = queuesLock { jobsStates.remove(job) }
    def submitted = queuesLock { jobsStates.toSeq.collect { case (j, SSHEnvironment.Submitted(id)) ⇒ (j, id) } }
    def queued = queuesLock { jobsStates.collect { case (job, Queued(desc, bj)) ⇒ (job, desc, bj) } }
  }

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: SSHJobService[_])(implicit services: BatchEnvironment.Services) =
    submitToCluster(
      batchExecutionJob,
      storage,
      space,
      jobService.register(batchExecutionJob, _, _, _),
      jobService.state(_),
      jobService.delete(_),
      jobService.stdOutErr(_)
    )

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
  val authentication:       A,
  val services:             BatchEnvironment.Services
) extends BatchEnvironment { env ⇒

  implicit def servicesImplicit = services
  import services._

  lazy val jobUpdater = new SSHJobService.Updater(WeakReference(this))

  type PID = Int
  lazy val submittedJobs = collection.mutable.Map[SSHEnvironment.SSHJob, PID]()

  lazy val stateRegistry = new SSHEnvironment.SSHJobStateRegistry

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = System()
  implicit val localInterpreter = gridscale.local.Local()

  def timeout = services.preference(SSHEnvironment.timeOut)

  override def start() = {
    storageService
    import services.threadProvider
    Updater.delay(jobUpdater, services.preference(SSHEnvironment.updateInterval))
  }

  override def stop() = {
    stopped = true
    cleanSSHStorage(storageService, background = false)
    jobUpdater.stop = true
    BatchEnvironment.waitJobKilled(this)
    sshInterpreter().close
  }

  import env.services.preference

  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))

  lazy val sshServer = gridscale.ssh.SSHServer(env.host, env.port, env.timeout)(env.authentication)

  lazy val storageService =
    if (storageSharedLocally) Left {
      val local = localStorage(env, sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
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

  def execute(batchExecutionJob: BatchExecutionJob) =
    storageService match {
      case Left((space, local)) ⇒ SSHEnvironment.submit(batchExecutionJob, local, space, sshJobService)
      case Right((space, ssh))  ⇒ SSHEnvironment.submit(batchExecutionJob, ssh, space, sshJobService)
    }

  lazy val installRuntime =
    storageService match {
      case Left((space, local)) ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), local, space.baseDirectory)
      case Right((space, ssh))  ⇒ new RuntimeInstallation(Frontend.ssh(host, port, timeout, authentication), ssh, space.baseDirectory)
    }

  lazy val sshJobService =
    storageService match {
      case Left((space, local)) ⇒ new SSHJobService(local, space.tmpDirectory, services, installRuntime, env, accessControl)
      case Right((space, ssh))  ⇒ new SSHJobService(ssh, space.tmpDirectory, services, installRuntime, env, accessControl)
    }

}

