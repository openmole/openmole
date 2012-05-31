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

package org.openmole.plugin.environment.jsaga

import java.util.UUID
import org.ogf.saga.job.JobDescription
import org.ogf.saga.job.JobFactory
import org.ogf.saga.job.JobService
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.UsageControl.withToken
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.batch.environment.Storage
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.file.URIFile
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.Runtime

trait SharedFSJobService { this: JSAGAJobService ⇒

  def sharedFS: Storage

  @transient private var installed: String = null

  def preparedRuntime(runtime: Runtime) = synchronized {
    if (installed == null) {
      installed = withToken(sharedFS.description, token ⇒ {

        val workdir = sharedFS.baseDir(token).mkdirIfNotExist("install")

        val script = Workspace.newFile("install", ".sh")
        val remoteScript = try {

          val tmpDirName = UUID.randomUUID.toString

          script.content =
            "if [ -d " + runtime.runtime.hash + " ]; then exit 0; fi; " +
              "mkdir " + tmpDirName + "; cd " + tmpDirName + "; if [ `uname -m` = x86_64 ]; then cp " + runtime.jvmLinuxX64.path + " jvm.tar.gz.gz;" +
              "else cp " + runtime.jvmLinuxI386.path + " jvm.tar.gz.gz; fi;" +
              "gunzip jvm.tar.gz.gz; gunzip jvm.tar.gz; tar -xf jvm.tar; rm jvm.tar;" +
              "cp " + runtime.runtime.path + " runtime.tar.gz.gz; gunzip runtime.tar.gz.gz; gunzip runtime.tar.gz; tar -xf runtime.tar; rm runtime.tar; mkdir envplugins; PLUGIN=0;" +
              runtime.environmentPlugins.map { p ⇒ "cp " + p.path + " envplugins/plugin$PLUGIN.jar.gz; gunzip envplugins/plugin$PLUGIN.jar.gz; PLUGIN=`expr $PLUGIN + 1`;" }.foldLeft("") { case (c, s) ⇒ c + s } +
              "cd ..; if [ -d " + runtime.runtime.hash + " ]; then rm -rf " + tmpDirName + "; exit 0; fi; " +
              " mv " + tmpDirName + " " + runtime.runtime.hash + "; ls | grep -v " + runtime.runtime.hash + " | xargs rm -rf"

          val remoteScript = workdir.newFileInDir("install", ".sh")
          URIFile.copy(script, remoteScript, token)
          remoteScript
        } finally script.delete

        //try {
        val name = remoteScript.name
        val install = JobFactory.createJobDescription
        install.setAttribute(JobDescription.EXECUTABLE, "/bin/bash")
        install.setVectorAttribute(JobDescription.ARGUMENTS, Array[String](name))
        install.setAttribute(JobDescription.WORKINGDIRECTORY, workdir.path)

        val job = jobService.createJob(install)
        job.run
        job.get
        //} finally remoteScript.remove(token)

        workdir.child(runtime.runtime.hash).path
      })
    }
    installed
  }

  def buildScript(serializedJob: SerializedJob, token: AccessToken) = {

    withToken(serializedJob.communicationStorage.description, {
      token ⇒
        val tmp = serializedJob.communicationStorage.tmpSpace(token)
        val result = tmp.newFileInDir("result", ".xml.gz")

        val script = Workspace.newFile("run", ".sh")
        val remoteScript = try {
          val workspace = UUID.randomUUID
          script.content =
            "export PATH=" + installed + "/jvm/bin/" + ":$PATH; cd " + installed + "; mkdir " + workspace + "; " +
              "sh run.sh " + environment.runtimeMemory + "m " + UUID.randomUUID + " -s file:/" +
              " -c " + serializedJob.communicationDirPath + " -p envplugins/ -i " + serializedJob.inputFilePath + " -o " + result.path +
              " -w " + workspace + "; rm -rf " + workspace + ";"

          val remoteScript = tmp.newFileInDir("run", ".sh")
          URIFile.copy(script, remoteScript, token)
          remoteScript
        } finally script.delete
        (remoteScript, result)
    })

  }

}

