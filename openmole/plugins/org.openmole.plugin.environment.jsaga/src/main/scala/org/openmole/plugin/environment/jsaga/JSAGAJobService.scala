/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.jsaga

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.ogf.saga.task.TaskMode
import org.ogf.saga.url.URLFactory
import org.openmole.core.batch.control.JobServiceDescription
import org.openmole.core.batch.environment.Authentication
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.io.FileUtil._
import scala.io.Source._

object JSAGAJobService extends Logger {
  val CreationTimeout = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "CreationTimout")
  val TestJobDoneTimeOut = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "TestJobDoneTimeOut")

  Workspace += (CreationTimeout, "PT2M")
  Workspace += (TestJobDoneTimeOut, "PT30M")
}

abstract class JSAGAJobService(jobServiceURI: URI, environment: JSAGAEnvironment, nbAccess: Int) extends JobService(environment, new JobServiceDescription(jobServiceURI.toString), nbAccess) {

  import JSAGAJobService._
  
  override def test: Boolean =
    try {
      val hello = JSAGAJobBuilder.helloWorld
      val job = jobServiceCache.createJob(hello)

      job.run
      job.getState
      //job.cancel();
      true
    } catch {
      case e => 
        logger.log(FINE, jobServiceURI + ": " + e.getMessage, e)
        false
    } 

  @transient lazy val jobServiceCache = {
    val task = {
      val url = URLFactory.createURL(jobServiceURI.toString)
      JobFactory.createJobService(TaskMode.ASYNC, JSAGASessionService.session(jobServiceURI.toString), url)
    } 

    task.get(Workspace.preferenceAsDurationInMs(JSAGAJobService.CreationTimeout), TimeUnit.MILLISECONDS);
  }

}
