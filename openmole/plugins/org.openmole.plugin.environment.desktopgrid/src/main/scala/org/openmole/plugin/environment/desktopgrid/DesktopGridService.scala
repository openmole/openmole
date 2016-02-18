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

package org.openmole.plugin.environment.desktopgrid

import java.io.File
import java.net.URI
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.batch.replication.ReplicaCatalog
import org.openmole.core.batch.storage._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.GridScaleStorage
import org.openmole.plugin.tool.sftpserver.SFTPServer
import org.openmole.tool.file._
import org.openmole.core.batch.jobservice._
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.tool.file._
import org.openmole.tool.thread._

import DesktopGridEnvironment._

object DesktopGridService {

  case class DesktopGridEntry(service: DesktopGridService, var users: Int)

  val services = new collection.mutable.HashMap[Int, DesktopGridEntry]()

  def borrow(port: Int): DesktopGridService = services.synchronized {
    val service = services.getOrElseUpdate(port, DesktopGridEntry(DesktopGridService(port), 0))
    service.users += 1
    service.service
  }

  def release(port: Int) = services.synchronized {
    services.get(port).foreach { service ⇒
      service.users -= 1
      if (service.users == 0) {
        services.remove(port)
        service.service.clean
      }
    }
  }

  def apply(port: Int) = {
    val service = new DesktopGridService(port)
    ReplicaCatalog.deleteReplicas(service.storageId)
    service
  }

}

trait DesktopGridJobService extends JobService {
  override type J = String
}

class DesktopGridService(port: Int, path: File = Workspace.newDir()) { service ⇒
  type J = String

  val url = new URI("desktop", null, "localhost", port, null, null, null)
  val storageId = url.toString

  def storage(_environment: BatchEnvironment, port: Int) = new StorageService with GridScaleStorage with CompressedTransfer {
    val usageControl = new UnlimitedAccess
    val remoteStorage: RemoteStorage = new DumyStorage
    val environment = _environment
    def root = "/"
    val storage = new RelativeStorage(path)
    val id = storageId
    val url = service.url

    override def persistentDir(implicit token: AccessToken) = baseDir(token)
    override def tmpDir(implicit token: AccessToken) = baseDir(token)
  }

  def jobService(_environment: BatchEnvironment, port: Int) = new DesktopGridJobService {
    override protected def _purge(j: J): Unit = service.purge(j)
    override protected def _submit(serializedJob: SerializedJob): BatchJob = service.submit(serializedJob, environment.openMOLEMemoryValue, this)
    override protected def _state(j: J): ExecutionState = service.state(j)
    override protected def _cancel(j: J): Unit = service.cancel(j)
    override def environment: BatchEnvironment = _environment
    override val usageControl = new UnlimitedAccess
  }

  val server = new SFTPServer(path, port, DesktopGridAuthentication.password)

  val timeStempsDir = new File(path, timeStempsDirName) { mkdirs }
  val jobsDir = new File(path, jobsDirName) { mkdirs }
  val tmpJobsDir = new File(path, tmpJobsDirName) { mkdirs }
  val resultsDir = new File(path, resultsDirName) { mkdirs }
  val tmpResultsDir = new File(path, tmpResultsDirName) { mkdirs }

  def jobSubmissionFile(jobId: String) = new File(jobsDir, jobId)
  def tmpJobSubmissionFile(jobId: String) = new File(tmpJobsDir, jobId)
  def timeStemps(jobId: String) = timeStempsDir.listFilesSafe.filter { _.getName.startsWith(jobId) }
  def timeStempsExists(jobId: String) = timeStempsDir.listFilesSafe.exists { _.getName.startsWith(jobId) }
  def resultExists(jobId: String) = resultsDir.listFilesSafe.exists { _.getName.startsWith(jobId) }
  def results(jobId: String) = resultsDir.listFilesSafe.filter { _.getName.startsWith(jobId) }

  def submit(serializedJob: SerializedJob, memory: Int, js: DesktopGridJobService): BatchJob = synchronized {
    server.startIfNeeded
    val jobId = new File(serializedJob.path).getName
    import serializedJob._
    val desktopJobMessage = new DesktopGridJobMessage(runtime.runtime, runtime.environmentPlugins, memory, inputFile)

    val tmpJobFile = tmpJobSubmissionFile(jobId)
    tmpJobFile.withGzippedOutputStream(os ⇒
      SerialiserService.serialise(desktopJobMessage, os)
    )

    tmpJobFile.move(jobSubmissionFile(jobId))

    new BatchJob with BatchJobId {
      val id = jobId
      val jobService = js
      def resultPath = s"$resultsDirName/${results(jobId).head.getName}"
    }
  }

  def state(j: J): ExecutionState = {
    if (!timeStempsExists(j)) SUBMITTED
    else if (!resultExists(j)) RUNNING
    else DONE
  }

  def cancel(j: J) = {}

  def purge(j: J) = {
    tmpJobSubmissionFile(j).delete
    jobSubmissionFile(j).delete
    timeStemps(j).foreach { _.delete }
    results(j).foreach { _.delete }
  }

  def empty = jobsDir.listFilesSafe.isEmpty

  def clean = {
    server.stop
    path.recursiveDelete
    ReplicaCatalog.deleteReplicas(storageId)
  }

}
