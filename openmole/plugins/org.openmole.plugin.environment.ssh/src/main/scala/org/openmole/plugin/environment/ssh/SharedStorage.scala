/*
 * Copyright (C) 2012 Romain Reuillon
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
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.tools.service.Logger
import org.openmole.core.workspace.Workspace
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.storage._
import FileUtil._
import concurrent.duration._
import scala.util.Try

object SharedStorage extends Logger

import SharedStorage._
import Log._

trait SharedStorage extends SSHService { js ⇒
  def sharedFS: SSHStorageService

  def installJobService = new fr.iscpif.gridscale.ssh.SSHJobService {
    def credential = js.credential
    def host = js.host
    def user = js.user
    override def port = js.port
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout)
  }

  @transient private var installed: Option[String] = None

  def preparedRuntime(runtime: Runtime) = synchronized {
    installed match {
      case None ⇒ sharedFS.withToken { implicit token ⇒
        val runtimePrefix = "runtime"
        val runtimeInstall = runtimePrefix + runtime.runtime.hash

        val (workdir, scriptName) = {
          val workdir = sharedFS.child(sharedFS.root, Workspace.preference(Workspace.uniqueID) + "_install")
          if (!sharedFS.exists(workdir)) sharedFS.makeDir(workdir)

          val script = Workspace.newFile("install", ".sh")
          try {
            val tmpDirName = runtimePrefix + UUID.randomUUID.toString
            val scriptName = Storage.uniqName("install", ".sh")

            val content =
              s"if [ -d $runtimeInstall ]; then exit 0; fi; " +
                s"mkdir $tmpDirName; cd $tmpDirName; if [ `uname -m` = x86_64 ]; then cp ${runtime.jvmLinuxX64.path} jvm.tar.gz.gz; " +
                s"else cp ${runtime.jvmLinuxI386.path} jvm.tar.gz.gz; fi;" +
                "gunzip jvm.tar.gz.gz; gunzip jvm.tar.gz; tar -xf jvm.tar; rm jvm.tar;" +
                s"cp ${runtime.runtime.path} runtime.tar.gz.gz; gunzip runtime.tar.gz.gz; gunzip runtime.tar.gz; tar -xf runtime.tar; rm runtime.tar; mkdir envplugins; PLUGIN=0;" +
                runtime.environmentPlugins.map { p ⇒ "cp " + p.path + " envplugins/plugin$PLUGIN.jar.gz; gunzip envplugins/plugin$PLUGIN.jar.gz; PLUGIN=`expr $PLUGIN + 1`;" }.foldLeft("") { case (c, s) ⇒ c + s } +
                s"cd ..; if [ -d $runtimeInstall ]; then rm -rf $tmpDirName; exit 0; fi; " +
                s"mv $tmpDirName $runtimeInstall ; rm -rf $tmpDirName ; rm $scriptName ; ls $runtimePrefix* | grep -v '^$runtimeInstall' | xargs rm -rf "

            logger.fine(s"Install script: $content")

            script.content = content

            val remoteScript = sharedFS.child(workdir, scriptName)
            sharedFS.upload(script, remoteScript)
            (workdir, scriptName)
          }
          finally script.delete
        }

        val jobDescription = new fr.iscpif.gridscale.ssh.SSHJobDescription {
          val executable = "/bin/bash"
          val arguments = scriptName
          val workDirectory = workdir
        }

        logger.fine("Begin install")
        installJobService.execute(jobDescription)
        logger.fine("End install")

        val path = sharedFS.child(workdir, runtimeInstall)
        installed = Some(path)
        path
      }
      case Some(path) ⇒ path
    }
  }

  protected def buildScript(serializedJob: SerializedJob) = {
    val runtime = preparedRuntime(serializedJob.runtime)
    val result = sharedFS.child(serializedJob.path, Storage.uniqName("result", ".xml.gz"))

    val script = Workspace.newFile("run", ".sh")
    val remoteScript = try {
      val workspace = serializedJob.storage.child(serializedJob.path, UUID.randomUUID.toString)
      val osgiWorkDir = serializedJob.storage.child(serializedJob.path, UUID.randomUUID.toString)

      val content =
        "export PATH=" + runtime + "/jre/bin/" + ":$PATH; cd " + runtime + "; export OPENMOLE_HOME=" + workspace + " ; mkdir $OPENMOLE_HOME ; " +
          "sh run.sh " + environment.openMOLEMemoryValue + "m " + osgiWorkDir + " -s " + serializedJob.runtime.storage.path +
          " -c " + serializedJob.path + " -p envplugins/ -i " + serializedJob.inputFile + " -o " + result + " -t " + environment.threadsValue +
          "; rm -rf $OPENMOLE_HOME ; rm -rf " + osgiWorkDir + " ;"

      logger.fine("Script: " + content)

      script.content = content

      val remoteScript = sharedFS.child(serializedJob.path, Storage.uniqName("run", ".sh"))
      sharedFS.withToken { sharedFS.upload(script, remoteScript)(_) }
      remoteScript
    }
    finally script.delete
    (remoteScript, result)
  }

}

