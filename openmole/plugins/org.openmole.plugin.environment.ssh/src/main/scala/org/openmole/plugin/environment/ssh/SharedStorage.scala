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
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.environment.batch.environment._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.core.communication.storage._
import org.openmole.core.exception.InternalProcessingError
import org.openmole.plugin.environment.batch.refresh.{ JobManager, RetryAction }
import org.openmole.tool.logger.JavaLogger
import squants.information.Information

object SharedStorage extends JavaLogger:

  def installRuntime[S](runtime: Runtime, storage: S, frontend: Frontend, baseDirectory: String)(using preference: Preference, newFile: TmpDirectory, storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], priority: AccessControl.Priority) =
    val runtimePrefix = "runtime"
    val runtimeInstall = runtimePrefix + runtime.runtime.hash

    val (workdir, scriptName) =
      val workdir = StorageService.child(storage, baseDirectory, "install")
      util.Try(hierarchicalStorageInterface.makeDir(storage, workdir))

      newFile.withTmpFile("install", ".sh"): script =>

        val tmpDirName = runtimePrefix + UUID.randomUUID.toString
        val scriptName = uniqName("install", ".sh")

        val cleanOld =
          s"""
             |function cleanOtherRuntime(){
             |TARGET_DIR="$$1"
             |find "$$TARGET_DIR" -maxdepth 1 -type d -name "$runtimePrefix*" ! -name "$runtimeInstall" | while read -r dir; do
             |  rm -rf $$dir
             |done
             |}
             |""".stripMargin

        val content =
          cleanOld +
          s"{ if [ -d $runtimeInstall ]; then rm $scriptName ;exit 0; fi; } && " +
            s"mkdir -p $tmpDirName && cd $tmpDirName && { if [ `uname -m` = x86_64 ]; then cp ${runtime.jvmLinuxX64.path} jvm.tar.gz; " +
            """else echo "Unsupported architecture: " `uname -m`; exit 1; fi; } && """ +
            "gunzip jvm.tar.gz && tar -xf jvm.tar && rm jvm.tar && " +
            s"cp ${runtime.runtime.path} runtime.tar.gz && gunzip runtime.tar.gz && tar -xf runtime.tar; rm runtime.tar && " +
            s"mkdir -p envplugins && PLUGIN=0 && " +
            runtime.environmentPlugins.map { p => "cp " + p.path + " envplugins/plugin$PLUGIN.jar && PLUGIN=`expr $PLUGIN + 1`" }.mkString(" && ") + " && " +
            s"PATH=$$PWD/jre/bin:$$PATH /bin/bash -x run.sh 256m test_run --test && rm -rf test_run && " +
            s"cd .. && { if [ -d $runtimeInstall ]; then rm -rf $tmpDirName; exit 0; fi } && " +
            s"mv $tmpDirName $runtimeInstall && rm -rf $tmpDirName && rm $scriptName && cleanOtherRuntime $workdir"

        Log.logger.fine(s"Install script: $content")

        script.content = content

        val remoteScript = StorageService.child(storage, workdir, scriptName)
        storageInterface.upload(storage, script, remoteScript, options = TransferOptions(raw = true, noLink = true, canMove = true))
        (workdir, scriptName)

    val command = s"""cd "$workdir" ; /bin/bash -x $scriptName"""

    Log.logger.fine("Begin install")

    frontend.run(command) match
      case util.Failure(e) => throw new InternalProcessingError(e, "There was an error during the runtime installation process.")
      case util.Success(r) =>
        r.returnCode match
          case 0 =>
          case _ =>
            throw new InternalProcessingError(s"Unexpected return status for the install process ${r.returnCode}.\nstdout:\n${r.stdOut}\nstderr:\n${r.stdErr}")

    val path = StorageService.child(storage, workdir, runtimeInstall)

    //installJobService.execute(jobDescription)
    Log.logger.fine("End install")

    path

  object JobScript:
    def defaultModules = Seq("apptainer", "singularity")

  case class JobScript(content: String, jobWorkDirectory: String):
    override def toString = content

  def buildScript[S](
    runtimePath:    Runtime => String,
    jobDirectory:   String,
    workDirectory:  String,
    openMOLEMemory: Option[Information],
    serializedJob:  SerializedJob,
    outputPath:     String,
    storage:        S,
    modules:        Option[Seq[String]],
    debug:          Boolean             = false)(using newFile: TmpDirectory, preference: Preference, storageInterface: StorageInterface[S], hierarchicalStorageInterface: HierarchicalStorageInterface[S], priority: AccessControl.Priority) =
    val runtime = runtimePath(serializedJob.runtime) //preparedRuntime(serializedJob.runtime)
    val result = outputPath
    val workspace = StorageService.child(storage, workDirectory, s"openmole_${UUID.randomUUID.toString}")

    val remoteScript =
      newFile.withTmpFile("run", ".sh"): script =>

        val loadModules = modules.getOrElse(JobScript.defaultModules).map(m => s"module load $m")

        val commands =
          Seq(s"trap 'cleanup_work_directory' SIGTERM")
          loadModules ++
            Seq(
              s"export PATH=$runtime/jre/bin/:$$PATH",
              s"cd $runtime",
              s"mkdir -p $workspace",
              s"sh run.sh ${BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt}m $workspace -s ${serializedJob.remoteStorage.path}" +
                s" -p envplugins/ -i ${serializedJob.inputPath} -o $result" + (if debug then " --debug" else ""),
              "RETURNCODE=$?",
              s"rm -rf $workspace",
              "exit $RETURNCODE"
            )

        val content =
          s"""
            |function cleanup_work_directory(){
            |  sleep 20
            |  rm -rf $workspace
            |  exit 15
            |}
            |
            |${commands.mkString(" ; ")}
            |""".stripMargin

        Log.logger.fine("Script: " + content)

        script.content = content

        val remoteScript = StorageService.child(storage, jobDirectory, uniqName("run", ".sh"))
        StorageService.upload(storage, script, remoteScript, options = TransferOptions(raw = true, noLink = true, canMove = true))
        remoteScript

    JobScript(remoteScript, workspace)


