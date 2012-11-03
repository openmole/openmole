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

package org.openmole.plugin.environment.gridscale

import fr.iscpif.gridscale.storage.SSHStorage
import java.util.UUID
import org.openmole.core.batch.control._
import org.openmole.core.batch.environment._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace._
import org.openmole.misc.exception._
import org.openmole.core.batch.jobservice._
import org.openmole.core.batch.storage._
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.gridscale.authentication.SSHAuthentication
import fr.iscpif.gridscale.jobservice.{ SSHJobService, SSHJobDescription, JobService ⇒ GSJobService, Done }
import fr.iscpif.gridscale.jobservice.untilFinished

object SharedStorage {
  val UpdateInstallJobInterval = new ConfigurationLocation("SharedStorage", "UpdateInstallJobInterval")
  Workspace += (UpdateInstallJobInterval, "PT5S")
}

import SharedStorage._

trait SharedStorage extends SSHService { js ⇒
  def sharedFS = new SimpleStorage {
    val storage = new SSHStorage {
      def host = js.host
      def user = js.user
      override def port = js.port
    }
    val root = js.root
    def authentication = js.authentication
  }

  def installJobService: SSHJobService = new SSHJobService {
    def authentication = js.authentication
    def host = js.host
    def user = js.user
    override def port = js.port
  }

  def root: String

  @transient private var installed: Option[String] = None

  def preparedRuntime(runtime: Runtime) = synchronized {
    installed match {
      case None ⇒
        val (workdir, scriptName) = {
          val workdir = sharedFS.child(sharedFS.root, "install")
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
          } finally script.delete
        }
        try {
          val jobDescription = new SSHJobDescription {
            val executable = "/bin/bash"
            val arguments = scriptName
            val workDirectory = workdir
          }

          val j = installJobService.submit(jobDescription)(authentication)
          val s = untilFinished { Thread.sleep(Workspace.preferenceAsDurationInMs(UpdateInstallJobInterval)); installJobService.state(j)(authentication) }

          if (s != Done) throw new InternalProcessingError("Installation of runtime has failed.")
        } finally sharedFS.rmFile(sharedFS.child(workdir, scriptName))

        val path = sharedFS.child(workdir, runtime.runtime.hash)
        installed = Some(path)
        path
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
          " -c " + serializedJob.path + " -p envplugins/ -i " + serializedJob.inputFile + " -o " + result +
          "; rm -rf $OPENMOLE_HOME ; rm -rf " + osgiWorkDir + " ;"

      val remoteScript = sharedFS.child(serializedJob.path, Storage.uniqName("run", ".sh"))
      sharedFS.upload(script, remoteScript)
      remoteScript
    } finally script.delete
    (remoteScript, result)
  }

}

