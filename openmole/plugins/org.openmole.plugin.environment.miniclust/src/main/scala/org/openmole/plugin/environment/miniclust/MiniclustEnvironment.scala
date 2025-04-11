package org.openmole.plugin.environment.miniclust

/*
 * Copyright (C) 2025 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.environment.batch.environment.*
import org.openmole.plugin.environment.batch.environment.AccessControl.*
import org.openmole.core.communication.storage.*
import org.openmole.core.communication.message.*
import _root_.gridscale.miniclust
import org.openmole.plugin.environment.gridscale.GridScaleJobService

import java.util.UUID

object MiniclustEnvironment:
  def apply(
    login: String,
    url: String,
    insecure: Boolean = false,
    openMOLEMemory: OptionalArgument[Information] = None,
    runtimeSetting: OptionalArgument[RuntimeSetting] = None,
    debug: Boolean = false)(using varName: sourcecode.Name, store: AuthenticationStore, pref: Preference, cypher: Cypher, replicaCatalog: ReplicaCatalog) =
    EnvironmentBuilder: ms =>
      new MiniclustEnvironment(
        url = url,
        insecure = insecure,
        openMOLEMemory = openMOLEMemory,
        name = Some(varName.value),
        authentication = MiniclustAuthentication.find(login, url),
        runtimeSetting = runtimeSetting,
        debug = debug,
        services = BatchEnvironment.Services(ms)
      )

  def toMiniclust(authentication: MiniclustAuthentication, insecure: Boolean) =
    val loginPassword =
      authentication match
        case a: MiniclustAuthentication.LoginPassword => a

    val auth = _root_.gridscale.authentication.UserPassword(loginPassword.login, loginPassword.password)

    val server =
      _root_.gridscale.miniclust.MiniclustServer(loginPassword.url, auth, insecure = insecure)

    _root_.gridscale.miniclust.Miniclust(server)


class MiniclustEnvironment(
  val url: String,
  val insecure: Boolean,
  val openMOLEMemory: Option[Information],
  val runtimeSetting: Option[RuntimeSetting],
  val name:              Option[String],
  val authentication: MiniclustAuthentication,
  val debug: Boolean,
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState(services)):

  given mc: _root_.gridscale.miniclust.Miniclust = MiniclustEnvironment.toMiniclust(authentication, insecure = insecure)

  override def execute(batchExecutionJob: BatchExecutionJob)(using Priority): BatchJobControl =
    import services.*

    // FIXME use storage service
    val persistentDirectory = "/openmole/persistent"
    val tmpDirectory = s"/openmole/tmp/${uniqName("", "")}"

    def replicate =
      BatchEnvironment.toReplicatedFile(
        upload = (p, t) => MiniclustStorage.upload(s"${persistentDirectory}/$p", t),
        exist = MiniclustStorage.exists,
        remove = MiniclustStorage.remove,
        environment = this,
        storageId = MiniclustStorage.id
      )

    val serializedJob =
      BatchEnvironment.serializeJob(
        environment = this,
        runtimeSetting = runtimeSetting,
        job = batchExecutionJob,
        remoteStorage = MiniclustStorage.Remote(),
        replicate = replicate,
        upload = (p, t) => MiniclustStorage.upload(s"$tmpDirectory/$p", t),
        storageId = MiniclustStorage.id,
        archiveResult = true
      )

    val stdout = uniqName("out", ".txt")
    val stderr = uniqName("err", ".txt")
    val scriptName = uniqName("script", ".sh")
    val resultName = uniqName("result", ".bin")

    val runScriptPath =
      TmpDirectory.withTmpFile("script", ".sh"): script =>
        import MiniclustStorage.nodeInputPath
        val runtime =
          Seq(
            s"tar -xzf ${nodeInputPath(serializedJob.runtime.jvmLinuxX64.path)} >/dev/null",
            s"tar -xzf ${nodeInputPath(serializedJob.runtime.runtime.path)} >/dev/null",
            s"mkdir envplugins"
          )

        val plugins =
          for (plugin, index) <- serializedJob.runtime.environmentPlugins.zipWithIndex
          yield s"ln ${nodeInputPath(plugin.path)} envplugins/plugin$index.jar"

        val memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt

        val run =
          Seq(
            "export PATH=$PWD/jre/bin:$PATH",
            "export HOME=$PWD",
            s"""/bin/sh run.sh ${memory}m ${UUID.randomUUID} -s $${PWD}/${nodeInputPath(serializedJob.remoteStorage.path)} -p $${PWD}/envplugins/ --input-file $$PWD/${nodeInputPath(serializedJob.inputPath)} -o $${PWD}/${resultName}""" + (if debug then " -d 2>&1" else "")
          )

        script.content =
          (runtime ++ plugins ++ run).mkString(" && ")

        MiniclustStorage.upload(script, TransferOptions.default)

    val inputsFiles =
      val fileMessages: Seq[FileMessage] =
        (serializedJob.executionMessage.plugins.map(_.toFileMessage) ++
          serializedJob.executionMessage.files.map(_.toFileMessage) ++
          serializedJob.runtime.environmentPlugins ++
          Seq(serializedJob.remoteStorage, serializedJob.runtime.jvmLinuxX64, serializedJob.runtime.runtime)).toSeq

      // TODO make sure that files are hashed using blake 3

      import _root_.miniclust.message.InputFile
      fileMessages.map: r =>
        val nodePath = MiniclustStorage.nodeInputPath(r.path)
        InputFile(r.path, nodePath, Some(s"blake3:${r.hash}"))
      ++ Seq(
        InputFile(serializedJob.inputPath, MiniclustStorage.nodeInputPath(serializedJob.inputPath)),
        InputFile(runScriptPath, scriptName)
      )


    val outputFiles =
      Seq(
        _root_.miniclust.message.OutputFile(resultName, resultName)
      )

    val job = _root_.gridscale.miniclust.submit(
      _root_.gridscale.miniclust.MinclustJobDescription(
        command = s"bash -x ${scriptName}",
        inputFile = inputsFiles,
        outputFile = outputFiles,
        stdOut = Some(stdout),
        stdErr = Some(stderr)
      )
    )

    def downloadResult(r: String, local: File, options: TransferOptions, priority: AccessControl.Priority) =
      _root_.gridscale.miniclust.download(_root_.miniclust.message.MiniClust.User.jobOutputPath(job.id, r), local)

    def state =
      GridScaleJobService.translateStatus:
        _root_.gridscale.miniclust.state(job)
    
    def delete = _root_.gridscale.miniclust.cancel(job)
    def stdOutErr = (_root_.gridscale.miniclust.stdOut(job).getOrElse(""), _root_.gridscale.miniclust.stdErr(job).getOrElse(""))

    def clean =
      _root_.gridscale.miniclust.rmDir(tmpDirectory)
      _root_.gridscale.miniclust.clean(job)

    BatchJobControl(
      BatchEnvironment.defaultUpdateInterval(services.preference),
      MiniclustStorage.id,
      priority => state,
      priority => delete,
      priority => stdOutErr,
      downloadResult,
      resultName,
      priority => clean
    )

  override def start(): Unit = ()
  override def stop(): Unit = ()

