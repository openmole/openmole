/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.environment.desktop

import java.io.File
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceDescription
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.SerializedJob
import collection.JavaConversions._

class DesktopJobService(environment: DesktopEnvironment, description: JobServiceDescription) extends JobService(environment, description){
  import DesktopEnvironment._
  
  val timeStempsDir = new File(environment.path, timeStempsDirName){mkdirs}
  val jobsDir = new File(environment.path, jobsDirName){mkdirs}
  val resultsDir = new File(environment.path, resultsDirName){mkdirs}

  def jobSubmissionFile(jobId: String) = new File(jobsDir, jobId)
  def timeStemps(jobId: String) = timeStempsDir.listFiles.filter{_.getName.startsWith(jobId)}
  def timeStempsExists(jobId: String) = timeStempsDir.list.exists{_.startsWith(jobId)}
  def resultExists(jobId: String) = resultsDir.list.exists{_.startsWith(jobId)}

  
  override protected def doSubmit(serializedJob: SerializedJob, token: AccessToken): BatchJob = {
    val jobId = new File(serializedJob.communicationDirPath).getName
    jobSubmissionFile(jobId).createNewFile
    new DesktopJob(this, jobId) 
  }
  override def test = true
}
