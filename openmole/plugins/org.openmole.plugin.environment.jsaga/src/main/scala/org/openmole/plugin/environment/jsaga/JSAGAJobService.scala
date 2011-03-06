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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.jsaga

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import java.util.concurrent.TimeUnit
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.ogf.saga.task.TaskMode
import org.ogf.saga.job.JobService
import org.ogf.saga.url.URLFactory
import org.openmole.core.batch.control.BatchJobServiceDescription
import org.openmole.core.batch.environment.BatchAuthentication
import org.openmole.core.batch.environment.BatchAuthenticationKey
import org.openmole.core.batch.environment.BatchJob
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.BatchJobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.file.IURIFile
import org.openmole.core.batch.jsaga.JSAGASessionService
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import scala.io.Source._

object JSAGAJobService {
  val CreationTimeout = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "CreationTimout")
  val TestJobDoneTimeOut = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "TestJobDoneTimeOut")

  Workspace += (CreationTimeout, "PT2M")
  Workspace += (TestJobDoneTimeOut, "PT30M")
}

abstract class JSAGAJobService(jobServiceURI: URI, environment: JSAGAEnvironment, nbAccess: Int) extends BatchJobService(environment, new BatchJobServiceDescription(jobServiceURI.toString), nbAccess) {

  override def test: Boolean = {

    try {
      val hello = JSAGAJobBuilder.helloWorld
      val job = jobServiceCache.createJob(hello)

      job.run
      job.getState
      //job.cancel();
      return true

    } catch {
      case e => Logger.getLogger(JSAGAJobService.getClass.getName).log(Level.FINE, jobServiceURI + ": " + e.getMessage, e)
        return false
    } 
  }

  override protected def doSubmit(inputFile: IURIFile, outputFile: IURIFile, runtime: Runtime, token: AccessToken): BatchJob = {

    val script = Workspace.newFile("script", ".sh")
    try {
      val os = new BufferedOutputStream(new FileOutputStream(script))
      try {
        generateScriptString(inputFile.toString, outputFile.toString, runtime, environment.memorySizeForRuntime.intValue, os)
      } finally {
        os.close
      }

      //println(fromFile(script).getLines.mkString)
      
      val jobDescription = buildJobDescription(runtime, script, environment.attributes)
      val job = jobServiceCache.createJob(jobDescription)
      job.run
            
      val id = job.getAttribute(Job.JOBID)
      return buildJob(id.substring(id.lastIndexOf('[') + 1, id.lastIndexOf(']')))
    } finally {
      script.delete
    }
  }
 
  @transient lazy val jobServiceCache = {
    val task = {
      val url = URLFactory.createURL(jobServiceURI.toString());
      JobFactory.createJobService(TaskMode.ASYNC, JSAGASessionService.session, url);
    } 

    task.get(Workspace.preferenceAsDurationInMs(JSAGAJobService.CreationTimeout), TimeUnit.MILLISECONDS);
  }

  protected def buildJob(id: String): JSAGAJob = new JSAGAJob(id, this)
  
  protected def buildJobDescription(runtime: Runtime, script: File, attributes: Map[String, String]): JobDescription = {
    JSAGAJobBuilder.jobDescription(runtime, script, attributes)
  }
    
  protected def generateScriptString (in: String, out: String, runtime: Runtime, memorySizeForRuntime: Int, os: OutputStream)
    
}
