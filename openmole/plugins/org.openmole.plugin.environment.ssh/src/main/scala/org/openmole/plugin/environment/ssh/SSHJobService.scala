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

package org.openmole.plugin.environment.ssh

import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import java.net.URI

class SSHJobService(uri: URI, val environment: BatchEnvironment, override val nbAccess: Int) extends JSAGAJobService(uri) {

  protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    //preparedRuntime(serializedJob.runtime)
    new SSHBatchJob(description)
  }

  @transient private var installed: Runtime = null
  
  def preparedRuntime(runtime: Runtime) = synchronized {
    if(installed == null) {
      val install = JobFactory.createJobDescription

      val script = Workspace.newFile("install", ".sh")
      try {
        script.content = "echo work >itsworking"
        install.setVectorAttribute(JobDescription.FILETRANSFER, Array[String]("file:/" + 
                                                                              {if(script.getAbsolutePath.startsWith("/")) script.getAbsolutePath.tail else script.getAbsolutePath} + ">" + script.getName))
        val job = jobServiceCache.createJob(install)
        job.run
        //println("Executed job install")
      } finally script.delete
      installed = runtime
    }
    installed
  }
  
}
