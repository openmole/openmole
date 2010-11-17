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
import java.io.OutputStream
import java.net.URI
import java.util.logging.Level
import java.util.logging.Logger
import java.util.concurrent.TimeUnit;
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.ogf.saga.task.TaskMode
import org.ogf.saga.job.JobService
import org.ogf.saga.url.URLFactory
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.commons.tools.io.FileOutputStream
import org.openmole.core.batchservicecontrol.BatchJobServiceDescription
import org.openmole.core.implementation.execution.batch.BatchJobService
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchJob
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey
import org.openmole.core.model.execution.batch.IRuntime
import org.openmole.core.model.file.IURIFile
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.plugin.environment.jsaga.internal.Activator
import scala.io.Source._
import scala.collection.JavaConversions._

object JSAGAJobService {
  val CreationTimeout = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "CreationTimout")
  val TestJobDoneTimeOut = new ConfigurationLocation(JSAGAJobService.getClass.getSimpleName, "TestJobDoneTimeOut")

  Activator.getWorkspace.addToConfigurations(CreationTimeout, "PT2M")
  Activator.getWorkspace.addToConfigurations(TestJobDoneTimeOut, "PT30M")
  
}

abstract class JSAGAJobService[ENV <: JSAGAEnvironment, AUTH <: IBatchServiceAuthentication](jobServiceURI: URI, environment: ENV, key: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH, nbAccess: Int) extends BatchJobService[ENV, AUTH](environment, key, authentication, new BatchJobServiceDescription(jobServiceURI.toString), nbAccess) {

  override def test: Boolean = {

    try {
      val hello = JSAGAJobBuilder.helloWorld
      val job = jobServiceCache.createJob(hello)

      job.run
      job.getState
      //job.cancel();
      return true

      /* float timeOut = Activator.getWorkspace().getPreferenceAsDurationInS(TestJobDoneTimeOut);
       if(!job.waitFor(timeOut)) {
       job.cancel();
       return false;
       }
       State state = job.getState();
       return state == State.DONE;*/
    } catch {
      case e => Logger.getLogger(JSAGAJobService.getClass.getName).log(Level.FINE, e.getMessage, e)
        return false
    } 
  }

  override protected def doSubmit(inputFile: IURIFile, outputFile: IURIFile, runtime: IRuntime, token: IAccessToken): IBatchJob = {

    val script = Activator.getWorkspace.newFile("script", ".sh")
    try {
      val os = new BufferedOutputStream(new FileOutputStream(script))
      try {
        generateScriptString(inputFile.toString, outputFile.toString, runtime, environment.memorySizeForRuntime.intValue, os)
      } finally {
        os.close
      }

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
      JobFactory.createJobService(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), url);
    } 

    task.get(Activator.getWorkspace.getPreferenceAsDurationInMs(JSAGAJobService.CreationTimeout), TimeUnit.MILLISECONDS);
  }
    
  protected def buildJob(id: String): JSAGAJob = {
    new JSAGAJob(id, this)
  }
    
  protected def buildJobDescription(runtime: IRuntime, script: File, attributes: Map[String, String]): JobDescription = {
    JSAGAJobBuilder.getJobDescription(runtime, script, attributes)
  }
    
  protected def generateScriptString (in: String, out: String, runtime: IRuntime, memorySizeForRuntime: Int, os: OutputStream)
    
}
