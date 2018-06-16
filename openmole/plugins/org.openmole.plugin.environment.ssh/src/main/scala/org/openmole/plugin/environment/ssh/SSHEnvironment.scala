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

import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

import effectaside._
import gridscale.ssh
import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.threadprovider.{ IUpdatable, Updater }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution._
import org.openmole.plugin.environment.batch.jobservice._
import org.openmole.plugin.environment.gridscale.{ GridScaleJobService, LocalStorage, LogicalLinkStorage }
import org.openmole.tool.crypto._
import org.openmole.tool.lock._
import squants.information._
import squants.time.TimeConversions._
import org.openmole.plugin.environment.batch.environment._

import scala.ref.WeakReference

object SSHEnvironment {

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

  class Updater(environemnt: WeakReference[SSHEnvironment[_]]) extends IUpdatable {

    var stop = false

    def update() =
      if (stop) false
      else environemnt.get match {
        case Some(env) ⇒
          val nbSubmit = env.slots - env.numberOfRunningJobs
          val toSubmit = env.queuesLock { env.jobsStates.collect { case (job, Queued(desc)) ⇒ job → desc }.take(nbSubmit) }

          for {
            (job, desc) ← toSubmit
          } env.submit(job, desc)

          !stop
        case None ⇒ false
      }
  }

  implicit def asSSHServer[A: ssh.SSHAuthentication] = new AsSSHServer[SSHEnvironment[A]] {
    override def apply(t: SSHEnvironment[A]) = ssh.SSHServer(t.host, t.port, t.timeout)(t.authentication)
  }

  case class SSHJob(id: Long) extends AnyVal

  sealed trait SSHRunState
  case class Queued(description: gridscale.ssh.SSHJobDescription) extends SSHRunState
  case class Submitted(pid: gridscale.ssh.JobId) extends SSHRunState
  case object Failed extends SSHRunState

  implicit def isJobService[A]: JobServiceInterface[SSHEnvironment[A]] = new JobServiceInterface[SSHEnvironment[A]] {
    override type J = SSHJob

    override def submit(env: SSHEnvironment[A], serializedJob: SerializedJob): BatchJob[J] = env.register(serializedJob)
    override def state(env: SSHEnvironment[A], j: J): ExecutionState.ExecutionState = env.state(j)
    override def delete(env: SSHEnvironment[A], j: J): Unit = env.delete(j)
    override def stdOutErr(js: SSHEnvironment[A], j: SSHJob) = js.stdOutErr(j)
  }

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

  lazy val jobUpdater = new SSHEnvironment.Updater(WeakReference(this))

  override def start() = {
    BatchEnvironment.start(this)
    import services.threadProvider
    Updater.delay(jobUpdater, services.preference(SSHEnvironment.UpdateInterval))
  }

  implicit val sshInterpreter = gridscale.ssh.SSH()
  implicit val systemInterpreter = System()

  def timeout = services.preference(SSHEnvironment.TimeOut)

  override def stop() = {
    jobUpdater.stop = true
    def usageControls = List(storageService.usageControl, jobService.usageControl)
    try BatchEnvironment.clean(this, usageControls)
    finally sshInterpreter().close
  }

  import env.services.{ threadProvider, preference }

  lazy val storageService =
    sshStorageService(
      user = user,
      host = host,
      port = port,
      storage = env,
      environment = env,
      concurrency = preference(SSHEnvironment.MaxConnections),
      sharedDirectory = sharedDirectory,
      storageSharedLocally = storageSharedLocally
    )

  override def trySelectStorage(files: ⇒ Vector[File]) = BatchEnvironment.trySelectSingleStorage(storageService)

  val installRuntime = new RuntimeInstallation(
    storageService = storageService,
    frontend = Frontend.ssh(host, port, timeout, authentication)
  )

  def register(serializedJob: SerializedJob) = {
    def buildScript(serializedJob: SerializedJob) = {
      import services._
      SharedStorage.buildScript(
        env.installRuntime.apply,
        env.workDirectory,
        env.openMOLEMemory,
        env.threads,
        serializedJob,
        env.storageService
      )
    }

    val (remoteScript, result, workDirectory) = buildScript(serializedJob)
    val jobDescription = gridscale.ssh.SSHJobDescription(
      command = s"/bin/bash $remoteScript",
      workDirectory = workDirectory
    )

    val registered = queuesLock {
      val job = SSHEnvironment.SSHJob(jobId.getAndIncrement())
      jobsStates.put(job, SSHEnvironment.Queued(jobDescription))
      job
    }

    BatchJob(registered, result)
  }

  def submit(job: SSHEnvironment.SSHJob, description: gridscale.ssh.SSHJobDescription) =
    try {
      val id = gridscale.ssh.submit(env, description)
      queuesLock { jobsStates.put(job, SSHEnvironment.Submitted(id)) }
    }
    catch {
      case t: Throwable ⇒
        queuesLock { jobsStates.put(job, SSHEnvironment.Failed) }
        throw t
    }

  def state(job: SSHEnvironment.SSHJob) = queuesLock {
    jobsStates.get(job) match {
      case None                               ⇒ ExecutionState.DONE
      case Some(state: SSHEnvironment.Queued) ⇒ ExecutionState.SUBMITTED
      case Some(SSHEnvironment.Failed)        ⇒ ExecutionState.FAILED
      case Some(SSHEnvironment.Submitted(id)) ⇒ GridScaleJobService.translateStatus(gridscale.ssh.state(env, id))
    }
  }

  def delete(job: SSHEnvironment.SSHJob): Unit = {
    val jobState = queuesLock { jobsStates.remove(job) }
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ gridscale.ssh.clean(env, id)
      case _                                  ⇒
    }
  }

  def stdOutErr(j: SSHEnvironment.SSHJob) = {
    val jobState = queuesLock { jobsStates.get(j) }
    jobState match {
      case Some(SSHEnvironment.Submitted(id)) ⇒ (gridscale.ssh.stdOut(env, id), gridscale.ssh.stdErr(env, id))
      case _                                  ⇒ ("", "")
    }
  }

  def numberOfRunningJobs: Int = {
    val sshJobIds = env.queuesLock { env.jobsStates.toSeq.collect { case (j, SSHEnvironment.Submitted(id)) ⇒ id } }
    sshJobIds.toList.map(id ⇒ gridscale.ssh.SSHJobDescription.jobIsRunning(env, id)).count(_ == true)
  }

  val queuesLock = new ReentrantLock()
  val jobsStates = collection.mutable.TreeMap[SSHEnvironment.SSHJob, SSHEnvironment.SSHRunState]()(Ordering.by(_.id))
  val jobId = new AtomicLong()

  type PID = Int
  val submittedJobs = collection.mutable.Map[SSHEnvironment.SSHJob, PID]()

  lazy val jobService = BatchJobService(env, concurrency = preference(SSHEnvironment.MaxConnections))
  override def trySelectJobService() = BatchEnvironment.trySelectSingleJobService(jobService)
}
