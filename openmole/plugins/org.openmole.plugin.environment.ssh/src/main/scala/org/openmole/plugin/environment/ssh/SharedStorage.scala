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

import org.openmole.tool.file._
import org.openmole.core.preference.Preference
import org.openmole.core.workspace.NewFile
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.core.communication.storage._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.tool.logger.JavaLogger
import squants.information.Information

object SharedStorage extends JavaLogger {

  def installRuntime(runtime: Runtime, sharedFS: StorageService[_], frontend: Frontend)(implicit preference: Preference, newFile: NewFile) =
    UsageControl.withToken(sharedFS.usageControl) { implicit token ⇒
      val runtimePrefix = "runtime"
      val runtimeInstall = runtimePrefix + runtime.runtime.hash

      val (workdir, scriptName) = {
        val installDir = sharedFS.child(sharedFS.baseDir, "install")
        util.Try(sharedFS.makeDir(installDir))

        val workdir = sharedFS.child(installDir, preference(Preference.uniqueID) + "_install")
        if (!sharedFS.exists(workdir)) sharedFS.makeDir(workdir)

        newFile.withTmpFile("install", ".sh") { script ⇒

          val tmpDirName = runtimePrefix + UUID.randomUUID.toString
          val scriptName = uniqName("install", ".sh")

          val content =
            s"{ if [ -d $runtimeInstall ]; then exit 0; fi; } && " +
              s"mkdir -p $tmpDirName && cd $tmpDirName && { if [ `uname -m` = x86_64 ]; then cp ${runtime.jvmLinuxX64.path} jvm.tar.gz; " +
              """else echo "Unsupported architecture: " `uname -m`; exit 1; fi; } && """ +
              "gunzip jvm.tar.gz && tar -xf jvm.tar && rm jvm.tar && " +
              s"cp ${runtime.runtime.path} runtime.tar.gz && gunzip runtime.tar.gz && tar -xf runtime.tar; rm runtime.tar && " +
              s"mkdir -p envplugins && PLUGIN=0 && " +
              runtime.environmentPlugins.map { p ⇒ "cp " + p.path + " envplugins/plugin$PLUGIN.jar && PLUGIN=`expr $PLUGIN + 1`" }.mkString(" && ") + " && " +
              s"PATH=$$PWD/jre/bin:$$PATH /bin/bash -x run.sh 256m test_run --test && rm -rf test_run && " +
              s"cd .. && { if [ -d $runtimeInstall ]; then rm -rf $tmpDirName; exit 0; fi } && " +
              s"mv $tmpDirName $runtimeInstall && rm -rf $tmpDirName && rm $scriptName && { ls | grep '^$runtimePrefix' | grep -v '^$runtimeInstall' | xargs rm -rf ; }"

          Log.logger.fine(s"Install script: $content")

          script.content = content

          val remoteScript = sharedFS.child(workdir, scriptName)
          sharedFS.upload(script, remoteScript, options = TransferOptions(raw = true, forceCopy = true, canMove = true))
          (workdir, scriptName)
        }
      }

      val command = s"""cd "$workdir" ; /bin/bash -x $scriptName"""

      Log.logger.fine("Begin install")

      frontend.run(command) match {
        case util.Failure(e) ⇒ throw new InternalProcessingError(e, "There was an error during the runtime installation process.")
        case util.Success(r) ⇒
          r.returnCode match {
            case 0 ⇒
            case _ ⇒
              throw new InternalProcessingError(s"Unexpected return status for the install process ${r.returnCode}.\nstdout:\n${r.stdOut}\nstderr:\n${r.stdErr}")
          }
      }

      val path = sharedFS.child(workdir, runtimeInstall)

      //installJobService.execute(jobDescription)
      Log.logger.fine("End install")

      path
    }

  def buildScript(runtimePath: Runtime ⇒ String, workDirectory: Option[String], openMOLEMemory: Option[Information], threads: Option[Int], serializedJob: SerializedJob, sharedFS: StorageService[_])(implicit newFile: NewFile, preference: Preference) = {
    val runtime = runtimePath(serializedJob.runtime) //preparedRuntime(serializedJob.runtime)
    val result = sharedFS.child(serializedJob.path, uniqName("result", ".bin"))
    val baseWorkDirectory = workDirectory getOrElse serializedJob.path
    val workspace = serializedJob.storage.child(baseWorkDirectory, UUID.randomUUID.toString)
    val osgiWorkDir = serializedJob.storage.child(baseWorkDirectory, UUID.randomUUID.toString)

    val remoteScript =
      newFile.withTmpFile("run", ".sh") { script ⇒
        val content =
          s"""export PATH=$runtime/jre/bin/:$$PATH; cd $runtime; mkdir -p $osgiWorkDir; export OPENMOLE_HOME=$workspace ; mkdir -p $$OPENMOLE_HOME ; """ +
            "sh run.sh " + BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt + "m " + osgiWorkDir + " -s " + serializedJob.runtime.storage.path +
            " -c " + serializedJob.path + " -p envplugins/ -i " + serializedJob.inputFile + " -o " + result + " -t " + BatchEnvironment.threadsValue(threads) +
            "; RETURNCODE=$?; rm -rf $OPENMOLE_HOME ; rm -rf " + osgiWorkDir + " ; exit $RETURNCODE;"

        Log.logger.fine("Script: " + content)

        script.content = content

        val remoteScript = sharedFS.child(serializedJob.path, uniqName("run", ".sh"))
        UsageControl.withToken(sharedFS.usageControl) { sharedFS.upload(script, remoteScript, options = TransferOptions(raw = true, forceCopy = true, canMove = true))(_) }
        remoteScript
      }
    (remoteScript, result, baseWorkDirectory)
  }

}
//
//import org.openmole.plugin.environment.ssh.SharedStorage._
//import Log._
//import org.openmole.core.communication.storage._
//import squants.time.TimeConversions._
//
//trait SharedStorage extends SSHService { js ⇒
//
//  def sharedFS: StorageService
//  def workDirectory: Option[String]
//
//  def installJobService = new fr.iscpif.gridscale.ssh.SSHJobService {
//    def credential = js.credential
//    def host = js.host
//    def user = js.user
//    override def port = js.port
//    override def timeout = environment.preference(SSHService.timeout)
//  }
//
//  @transient private var installed: Option[String] = None
//
//  def preparedRuntime(runtime: Runtime) = synchronized {
//    try installed match {
//      case None ⇒ sharedFS.withToken { implicit token ⇒
//        val runtimePrefix = "runtime"
//        val runtimeInstall = runtimePrefix + runtime.runtime.hash
//
//        val (workdir, scriptName) = {
//          val installDir = sharedFS.child(sharedFS.root, "install")
//          Try(sharedFS.makeDir(installDir))
//          val workdir = sharedFS.child(installDir, environment.preference(Preference.uniqueID) + "_install")
//          if (!sharedFS.exists(workdir)) sharedFS.makeDir(workdir)
//
//          environment.services.newFile.withTmpFile("install", ".sh") { script ⇒
//
//            val tmpDirName = runtimePrefix + UUID.randomUUID.toString
//            val scriptName = uniqName("install", ".sh")
//
//            val content =
//              s"(if [ -d $runtimeInstall ]; then exit 0; fi) && " +
//                s"mkdir -p $tmpDirName && cd $tmpDirName && (if [ `uname -m` = x86_64 ]; then cp ${runtime.jvmLinuxX64.path} jvm.tar.gz; " +
//                """else echo "Unsupported architecture: " `uname -m`; exit 1; fi) && """ +
//                "gunzip jvm.tar.gz && tar -xf jvm.tar && rm jvm.tar && " +
//                s"cp ${runtime.runtime.path} runtime.tar.gz && gunzip runtime.tar.gz && tar -xf runtime.tar; rm runtime.tar && " +
//                s"mkdir -p envplugins && PLUGIN=0 && " +
//                runtime.environmentPlugins.map { p ⇒ "cp " + p.path + " envplugins/plugin$PLUGIN.jar && PLUGIN=`expr $PLUGIN + 1`" }.mkString(" && ") + " && " +
//                s"cd .. && (if [ -d $runtimeInstall ]; then rm -rf $tmpDirName; exit 0; fi) && " +
//                s"mv $tmpDirName $runtimeInstall && rm -rf $tmpDirName && rm $scriptName && ( ls $runtimePrefix* | grep -v '^$runtimeInstall' | xargs rm -rf )"
//
//            logger.fine(s"Install script: $content")
//
//            script.content = content
//
//            val remoteScript = sharedFS.child(workdir, scriptName)
//            sharedFS.upload(script, remoteScript, options = TransferOptions(raw = true, forceCopy = true, canMove = true))
//            (workdir, scriptName)
//          }
//        }
//
//        val jobDescription = fr.iscpif.gridscale.ssh.SSHJobDescription(
//          executable = "/bin/bash",
//          arguments = scriptName,
//          workDirectory = workdir
//        )
//
//        logger.fine("Begin install")
//        installJobService.execute(jobDescription)
//        logger.fine("End install")
//
//        val path = sharedFS.child(workdir, runtimeInstall)
//        installed = Some(path)
//        path
//      }
//      case Some(path) ⇒ path
//    } catch {
//      case e: Exception ⇒ throw new InternalProcessingError(e, "There was an error during the runtime installation process.")
//    }
//  }
//
//  protected def buildScript(serializedJob: SerializedJob) = {
//    val runtime = preparedRuntime(serializedJob.runtime)
//    val result = sharedFS.child(serializedJob.path, uniqName("result", ".bin"))
//
//    val remoteScript =
//      environment.services.newFile.withTmpFile("run", ".sh") { script ⇒
//        val baseWorkspace = workDirectory getOrElse serializedJob.path
//        val workspace = serializedJob.storage.child(baseWorkspace, UUID.randomUUID.toString)
//        val osgiWorkDir = serializedJob.storage.child(baseWorkspace, UUID.randomUUID.toString)
//
//        import environment.preference
//
//        val content =
//          s"""export PATH=$runtime/jre/bin/:$$PATH; cd $runtime; mkdir -p $osgiWorkDir; export OPENMOLE_HOME=$workspace ; mkdir -p $$OPENMOLE_HOME ; """ +
//            "sh run.sh " + BatchEnvironment.openMOLEMemoryValue(environment.openMOLEMemory)(preference).toMegabytes.toInt + "m " + osgiWorkDir + " -s " + serializedJob.runtime.storage.path +
//            " -c " + serializedJob.path + " -p envplugins/ -i " + serializedJob.inputFile + " -o " + result + " -t " + BatchEnvironment.threadsValue(environment.threads) +
//            "; RETURNCODE=$?; rm -rf $OPENMOLE_HOME ; rm -rf " + osgiWorkDir + " ; exit $RETURNCODE;"
//
//        logger.fine("Script: " + content)
//
//        script.content = content
//
//        val remoteScript = sharedFS.child(serializedJob.path, uniqName("run", ".sh"))
//        sharedFS.withToken { sharedFS.upload(script, remoteScript, options = TransferOptions(raw = true, forceCopy = true, canMove = true))(_) }
//        remoteScript
//      }
//    (remoteScript, result)
//  }
//
//}
//
