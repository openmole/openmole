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
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace._
import org.openmole.misc.exception._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.storage._
import org.openmole.misc.tools.io.FileUtil._

object SharedStorage {
  val UpdateInstallJobInterval = new ConfigurationLocation("SharedStorage", "UpdateInstallJobInterval")
  Workspace += (UpdateInstallJobInterval, "PT5S")
}

import SharedStorage._

trait SharedStorage extends SSHService { js ⇒
  def sharedFS: SSHStorageService

  def installJobService = new fr.iscpif.gridscale.ssh.SSHJobService {
    def authentication = js.authentication
    def host = js.host
    def user = js.user
    override def port = js.port
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout).toMilliSeconds.toInt
  }

  @transient private var installed: Option[String] = None

  def preparedRuntime(runtime: Runtime) = synchronized {
    installed match {
      case None ⇒ sharedFS.withToken { implicit token ⇒
        val (workdir, scriptName) = {
          val workdir = sharedFS.child(sharedFS.root, Workspace.preference(Workspace.uniqueID) + "_install")
          if (!sharedFS.exists(workdir)) sharedFS.makeDir(workdir)

          val script = Workspace.newFile("install", ".sh")
          try {
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

            val scriptName = Storage.uniqName("install", ".sh")
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

        val j = installJobService.submit(jobDescription)(authentication)
        val s = fr.iscpif.gridscale.untilFinished { Thread.sleep(Workspace.preferenceAsDuration(UpdateInstallJobInterval).toMilliSeconds); installJobService.state(j)(authentication) }

        if (s != fr.iscpif.gridscale.Done) throw new InternalProcessingError("Installation of runtime has failed.")

        val path = sharedFS.child(workdir, runtime.runtime.hash)
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
      val workspace = UUID.randomUUID
      val osgiWorkDir = UUID.randomUUID
      script.content =
        "export PATH=" + runtime + "/jre/bin/" + ":$PATH; cd " + runtime + "; export OPENMOLE_HOME=$PWD/" + workspace + " ; mkdir $OPENMOLE_HOME ; " +
          "sh run.sh " + environment.openMOLEMemoryValue + "m " + osgiWorkDir + " -s " + serializedJob.runtime.storage.path +
          " -c " + serializedJob.path + " -p envplugins/ -i " + serializedJob.inputFile + " -o " + result + " -t " + environment.threadsValue +
          "; rm -rf $OPENMOLE_HOME ; rm -rf " + osgiWorkDir + " ;"

      val remoteScript = sharedFS.child(serializedJob.path, Storage.uniqName("run", ".sh"))
      sharedFS.withToken { sharedFS.upload(script, remoteScript)(_) }
      remoteScript
    }
    finally script.delete
    (remoteScript, result)
  }

}

