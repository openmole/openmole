/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.environment.ssh

import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.plugin.environment.batch.environment.SerializedJob
import org.openmole.plugin.environment.batch.jobservice.{ BatchJob, BatchJobService, JobServiceInterface }
//
//import java.util.concurrent.locks.{ Lock, ReentrantLock }
//
//import fr.iscpif.gridscale.ssh.{ SSHConnectionCache, SSHJobDescription, SSHJobService ⇒ GSSSHJobService }
//import org.openmole.core.preference.Preference
//import org.openmole.core.threadprovider.{ IUpdatable, ThreadProvider, Updater }
//import org.openmole.core.workflow.execution.ExecutionState._
//import org.openmole.plugin.environment.batch.environment._
//import org.openmole.plugin.environment.batch.storage._
//import org.openmole.plugin.environment.gridscale._
//import org.openmole.tool.logger.Logger
//import squants.time.TimeConversions._
//import org.openmole.tool.lock._
//import org.openmole.tool.thread._
//
//import scala.collection.mutable
//import scala.ref.WeakReference
//
//object SSHJobService extends Logger {
//
//  def apply(
//    slots:         Int,
//    sharedFS:      StorageService,
//    environment:   SSHEnvironment,
//    workDirectory: Option[String],
//    credential:    fr.iscpif.gridscale.ssh.SSHAuthentication,
//    host:          String,
//    user:          String,
//    port:          Int
//  )(implicit threadProvider: ThreadProvider, preference: Preference) = {
//    val (_slots, _sharedFS, _environment, _workDirectory, _credential, _host, _user, _port) =
//      (slots, sharedFS, environment, workDirectory, credential, host, user, port)
//
//    val js =
//      new SSHJobService {
//        def nbSlots = _slots
//        def sharedFS = _sharedFS
//        val environment = _environment
//        def workDirectory = _workDirectory
//        override def credential = _credential
//        override def host = _host
//        override def user = _user
//        override def port = _port
//      }
//
//    js
//  }
//

//
//}
//
//trait SSHJobService extends GridScaleJobService with SharedStorage { js ⇒
//
//  val environment: SSHEnvironment
//
//  def nbSlots: Int
//  override def usageControl = environment.usageControl
//
//  val jobService = new GSSSHJobService with SSHConnectionCache {
//    override def timeout = environment.preference(SSHService.timeout)
//    override def credential = environment.credential
//    override def host = environment.host
//    override def port = environment.port
//  }
//
//  val queuesLock = new ReentrantLock()
//  val queue = mutable.Stack[SSHBatchJob]()
//  val submitted = mutable.Stack[SSHBatchJob]()
//
//  protected def _submit(serializedJob: SerializedJob) = {
//    val (remoteScript, result) = buildScript(serializedJob)
//
//    val _jobDescription = SSHJobDescription(
//      executable = "/bin/bash",
//      arguments = remoteScript,
//      workDirectory = sharedFS.root
//    )
//
//    val sshBatchJob = new SSHBatchJob {
//      val jobService = js
//      val jobDescription = _jobDescription
//      val resultPath = result
//    }
//
//    SSHJobService.Log.logger.fine(s"SSHJobService: Queueing /bin/bash $remoteScript in directory ${sharedFS.root}")
//
//    queuesLock { queue.push(sshBatchJob) }
//
//    sshBatchJob
//  }
//
//  private[ssh] def submit(description: SSHJobDescription) =
//    jobService.submit(description)
//
//  lazy val jobUpdater = new SSHJobService.Updater(WeakReference(this))
//
//  def start() = {
//    import environment.preference
//    import environment.services.threadProvider
//    Updater.delay(jobUpdater, preference(SSHEnvironment.UpdateInterval))
//  }
//
//  def stop() = {
//    jobUpdater.stop = true
//  }
//
//}
//
//import freedsl.dsl._
//
//object SSHJobService {
//
//
//}
//
//case class SSHJobService(slots: Int)
//case class SSHJob(id: Long) extends AnyVal