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
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.tools.io.FileUtil
import collection.JavaConversions._
import org.openmole.core.batch.jobservice._
import org.openmole.core.serializer.SerialiserService
import FileUtil._
import org.openmole.core.workflow.execution.ExecutionState._

import DesktopGridEnvironment._

trait DesktopGridJobService extends JobService with UnlimitedAccess { js ⇒

  type J = String

  def environment: DesktopGridEnvironment

  val timeStempsDir = new File(environment.path, timeStempsDirName) { mkdirs }
  val jobsDir = new File(environment.path, jobsDirName) { mkdirs }
  val tmpJobsDir = new File(environment.path, tmpJobsDirName) { mkdirs }
  val resultsDir = new File(environment.path, resultsDirName) { mkdirs }
  val tmpResultsDir = new File(environment.path, tmpResultsDirName) { mkdirs }

  def jobSubmissionFile(jobId: String) = new File(jobsDir, jobId)
  def tmpJobSubmissionFile(jobId: String) = new File(tmpJobsDir, jobId)
  def timeStemps(jobId: String) = Option(timeStempsDir.listFiles).getOrElse(Array.empty).filter { _.getName.startsWith(jobId) }
  def timeStempsExists(jobId: String) = Option(timeStempsDir.list).getOrElse(Array.empty).exists { _.startsWith(jobId) }
  def resultExists(jobId: String) = Option(resultsDir.list).getOrElse(Array.empty).exists { _.startsWith(jobId) }
  def results(jobId: String) = Option(resultsDir.listFiles).getOrElse(Array.empty).filter { _.getName.startsWith(jobId) }

  protected def _submit(serializedJob: SerializedJob): BatchJob = {
    val jobId = new File(serializedJob.path).getName
    import serializedJob._
    val desktopJobMessage = new DesktopGridJobMessage(runtime.runtime, runtime.environmentPlugins, environment.openMOLEMemoryValue, inputFile)

    val tmpJobFile = tmpJobSubmissionFile(jobId)
    tmpJobFile.withGzippedOutputStream(os ⇒
      SerialiserService.serialise(desktopJobMessage, os)
    )

    tmpJobFile.move(jobSubmissionFile(jobId))

    new BatchJob with BatchJobId {
      val id = jobId
      val jobService = js
      def resultPath = results(jobId).head.getAbsolutePath
    }
  }

  protected def _state(j: J): ExecutionState = {
    if (!timeStempsExists(j)) SUBMITTED
    else if (!resultExists(j)) RUNNING
    else DONE
  }

  protected def _cancel(j: J) = {}

  protected def _purge(j: J) = {
    tmpJobSubmissionFile(j).delete
    jobSubmissionFile(j).delete
    timeStemps(j).foreach { _.delete }
    results(j).foreach { _.delete }
  }

}
