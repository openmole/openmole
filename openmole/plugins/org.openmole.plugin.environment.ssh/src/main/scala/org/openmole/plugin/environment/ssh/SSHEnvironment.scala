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
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.Updater
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.crypto._
import org.openmole.tool.lock._
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import squants.information._
import squants.time.TimeConversions._
import squants.time.Time
import scala.ref.WeakReference

object SSHEnvironment extends JavaLogger:

  val maxConnections = PreferenceLocation("SSHEnvironment", "MaxConnections", Some(5))
  val updateInterval = PreferenceLocation("SSHEnvironment", "UpdateInterval", Some(10 seconds))
  val timeOut = PreferenceLocation("SSHEnvironment", "Timeout", Some(1 minutes))
    
  def apply(
    user:                 String,
    host:                 String,
    slots:                Int,
    port:                 Int                           = 22,
    sharedDirectory:      OptionalArgument[String]      = None,
    workDirectory:        OptionalArgument[String]      = None,
    openMOLEMemory:       OptionalArgument[Information] = None,
    threads:              OptionalArgument[Int]         = None,
    killAfter:            OptionalArgument[Time]        = None,
    storageSharedLocally: Boolean                       = false,
    reconnect:            OptionalArgument[Time]        = SSHConnection.defaultReconnect,
    name:                 OptionalArgument[String]      = None,
    modules:              OptionalArgument[Seq[String]] = None,
    debug:                Boolean                       = false
  )(implicit cypher: Cypher, authenticationStore: AuthenticationStore, preference: Preference, serializerService: SerializerService, replicaCatalog: ReplicaCatalog, varName: sourcecode.Name) =

    EnvironmentBuilder: ms =>
      new SSHEnvironment(
        user = user,
        host = host,
        slots = slots,
        port = port,
        sharedDirectory = sharedDirectory,
        workDirectory = workDirectory,
        openMOLEMemory = openMOLEMemory,
        threads = threads,
        killAfter = killAfter,
        storageSharedLocally = storageSharedLocally,
        reconnect = reconnect,
        name = Some(name.getOrElse(varName.value)),
        authentication = SSHAuthentication.find(user, host, port),
        modules = modules,
        debug = debug,
        services = BatchEnvironment.Services(ms)
      )



  case class SSHJob(id: Long, workDirectory: String)

  sealed trait SSHRunState
  case class Queued(description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob) extends SSHRunState
  case class Submitted(pid: gridscale.ssh.JobId) extends SSHRunState
  case object Failed extends SSHRunState

  class SSHJobStateRegistry:
    val jobsStates = collection.mutable.TreeMap[SSHEnvironment.SSHJob, SSHEnvironment.SSHRunState]()(Ordering.by(_.id))
    val queuesLock = new ReentrantLock()
    val jobId = new AtomicLong()

    def registerJob(description: gridscale.ssh.SSHJobDescription, batchExecutionJob: BatchExecutionJob, jobWorkDirectory: String) = queuesLock:
      val job = SSHEnvironment.SSHJob(jobId.getAndIncrement(), jobWorkDirectory)
      jobsStates.put(job, SSHEnvironment.Queued(description, batchExecutionJob))
      job

    def update(job: SSHJob, state: SSHRunState) = queuesLock { jobsStates.put(job, state) }
    def get(job: SSHJob) = queuesLock { jobsStates.get(job) }
    def remove(job: SSHJob) = queuesLock { jobsStates.remove(job) }
    def submitted = queuesLock { jobsStates.toSeq.collect { case (j, SSHEnvironment.Submitted(id)) ⇒ (j, id) } }
    def queued = queuesLock { jobsStates.collect { case (job, Queued(desc, bj)) ⇒ (job, desc, bj) } }

  def submit[S: StorageInterface: HierarchicalStorageInterface: EnvironmentStorage](environment: BatchEnvironment, batchExecutionJob: BatchExecutionJob, storage: S, space: StorageSpace, jobService: SSHJobService[_])(using services: BatchEnvironment.Services, priority: AccessControl.Priority) =
    submitToCluster(
      environment,
      batchExecutionJob,
      storage,
      space,
      jobService.register(batchExecutionJob, _, _, _, _),
      jobService.state,
      jobService.delete,
      jobService.stdOutErr
    )


class SSHEnvironment(
  val user:                 String,
  val host:                 String,
  val slots:                Int,
  val port:                 Int,
  val sharedDirectory:      Option[String],
  val workDirectory:        Option[String],
  val openMOLEMemory:       Option[Information],
  val threads:              Option[Int],
  val killAfter:            Option[Time],
  val storageSharedLocally: Boolean,
  val reconnect:            Option[Time],
  val name:                 Option[String],
  val authentication:       SSHAuthentication,
  val modules:              Option[Seq[String]],
  val debug:                Boolean,
  val services:             BatchEnvironment.Services
) extends BatchEnvironment(BatchEnvironmentState(services)) { env ⇒

  implicit def servicesImplicit: BatchEnvironment.Services = services
  import services._

  lazy val jobUpdater = new SSHJobService.Updater(WeakReference(this))

  type PID = Int
  lazy val submittedJobs = collection.mutable.Map[SSHEnvironment.SSHJob, PID]()

  lazy val stateRegistry = new SSHEnvironment.SSHJobStateRegistry

  implicit val ssh: gridscale.ssh.SSH =
    lazy val sshServer = gridscale.ssh.SSHServer(env.host, env.port, env.timeout)(env.authentication)
    gridscale.ssh.SSH(sshServer, reconnect = reconnect)

  def timeout = services.preference(SSHEnvironment.timeOut)

  override def start() =
    storageService
    import services.threadProvider
    Updater.delay(jobUpdater, services.preference(SSHEnvironment.updateInterval))

  override def stop() =
    AccessControl.defaultPrirority:
      state.stopped = true
      cleanSSHStorage(storageService, background = false)
      jobUpdater.stop = true
      BatchEnvironment.waitJobKilled(this)
      ssh.close

  import env.services.preference

  lazy val accessControl = AccessControl(preference(SSHEnvironment.maxConnections))

 
  lazy val storageService =
    if storageSharedLocally
    then
      Left:
        val local = localStorage(env, sharedDirectory, AccessControl(preference(SSHEnvironment.maxConnections)))
        AccessControl.defaultPrirority:
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
            sharedDirectory = sharedDirectory
          )

        AccessControl.defaultPrirority:
          (sshStorageSpace(ssh), ssh)

  def execute(batchExecutionJob: BatchExecutionJob)(using AccessControl.Priority) =
    storageService match
      case Left((space, local)) ⇒ SSHEnvironment.submit(env, batchExecutionJob, local, space, sshJobService)
      case Right((space, ssh))  ⇒ SSHEnvironment.submit(env, batchExecutionJob, ssh, space, sshJobService)

  lazy val sshJobService =
    storageService match
      case Left((space, local)) ⇒ SSHJobService(local, space, services, env, accessControl)
      case Right((space, ssh))  ⇒ SSHJobService(ssh, space, services, env, accessControl)

}

