/*
 * Copyright (C) 2012 reuillon
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

import java.net.URI
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.environment.Storage
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import org.openmole.plugin.environment.jsaga.SharedFSJobService

class PBSJobService(
    val uri: URI,
    val sharedFS: Storage,
    val environment: PBSEnvironment,
    override val nbAccess: Int) extends JSAGAJobService with SharedFSJobService {

  protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    val (remoteScript, result) = buildScript(serializedJob, token)
    val jobDesc = JobFactory.createJobDescription
    jobDesc.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
    jobDesc.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](remoteScript.path))

    val job = jobService.createJob(jobDesc)
    job.run
    JSAGAJob(JSAGAJob.id(job), result.path, this)
  }

}
