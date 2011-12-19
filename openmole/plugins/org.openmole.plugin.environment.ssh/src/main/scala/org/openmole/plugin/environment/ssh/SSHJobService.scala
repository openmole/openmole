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

import java.util.UUID
import org.ogf.saga.job.Job
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.ServiceDescription
import org.openmole.core.batch.control.UsageControl._
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.Runtime
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.file.URIFile
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.jsaga.JSAGAJob
import org.openmole.plugin.environment.jsaga.JSAGAJobService
import java.net.URI

class SSHJobService(uri: URI, val environment: SSHEnvironment, override val nbAccess: Int) extends JSAGAJobService(uri) {

  protected def doSubmit(serializedJob: SerializedJob, token: AccessToken) = {
    val installed = preparedRuntime(serializedJob.runtime)
    val (remoteScript, result) = withToken(serializedJob.communicationStorage.description, {
        token => 
        val tmp = serializedJob.communicationStorage.tmpSpace(token)
        val result = tmp.newFileInDir("result", ".xml.gz")

        val script = Workspace.newFile("run", ".sh")
        val remoteScript = try {
          val workspace = UUID.randomUUID
          script.content = 
            "export PATH=" + installed + "/jvm/bin/" + ":$PATH; cd " + installed + "; mkdir " + workspace + "; " +
            "sh run.sh " + environment.memorySizeForRuntime + "m " + UUID.randomUUID + " -s file:/" + 
            " -c " + serializedJob.communicationDirPath + " -p envplugins/ -i " + serializedJob.inputFilePath + " -o " + result.path +
            " -w " + workspace + "; rm -rf " + workspace + ";"
            
          //println(script.content)
          val remoteScript = environment.storage.tmpSpace(token).newFileInDir("run", ".sh")
          URIFile.copy(script, remoteScript, token)
          remoteScript
        } finally script.delete
        (remoteScript, result)
      })
      val jobDesc = JobFactory.createJobDescription
      jobDesc.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
      jobDesc.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](remoteScript.path))
            
      val job = jobServiceCache.createJob(jobDesc)
      job.run
      new JSAGAJob(JSAGAJob.id(job), result.path, this)
  }

  @transient private var installed: String = null
  
  def preparedRuntime(runtime: Runtime) = synchronized {
    if(installed == null) {
      installed = withToken(environment.storage.description, token => {
          val workdir = environment.storage.baseDir(token)
          val script = Workspace.newFile("install", ".sh")
          val remoteScript = try {
            script.content = 
              "rm -rf runtime*; mkdir runtime; cd runtime; if [ `uname -m` = x86_64 ]; then cp " + runtime.jvmLinuxX64.path + " jvm.tar.gz.gz;" +
              "else cp " + runtime.jvmLinuxI386.path + " jvm.tar.gz.gz; fi;" +
              "gunzip jvm.tar.gz.gz; gunzip jvm.tar.gz; tar -xf jvm.tar; rm jvm.tar;" +
              "cp " + runtime.runtime.path + " runtime.tar.gz.gz; gunzip runtime.tar.gz.gz; gunzip runtime.tar.gz; tar -xf runtime.tar; rm runtime.tar; mkdir envplugins; PLUGIN=0;" +
              runtime.environmentPlugins.map{p => "cp " + p.path + " envplugins/plugin$PLUGIN.jar.gz; gunzip envplugins/plugin$PLUGIN.jar.gz; PLUGIN=`expr $PLUGIN + 1`;" }.foldLeft(""){case (c, s) => c + s} 
         //     "cp " + runtime.authentication.path + " authentication.xml.gz; gunzip authentication.xml.gz;"

            val remoteScript = workdir.newFileInDir("install", ".sh")
            URIFile.copy(script, remoteScript, token)
            remoteScript
          } finally script.delete
          
          try {
            val name = remoteScript.name(token)
            val install = JobFactory.createJobDescription
            install.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
            install.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](name))
            install.setAttribute(JobDescription.WORKINGDIRECTORY, workdir.path)
            
            val job = jobServiceCache.createJob(install)
            job.run
            job.get
          } finally remoteScript.remove(false, token)
 
          workdir.child("runtime/").path
        })
    }
    installed
  }
  
}
