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
import org.openmole.plugin.environment.batch.storage.*
import org.openmole.plugin.environment.batch.environment.AccessControl.*
import org.openmole.core.communication.storage.*
import org.openmole.core.communication.message.*
import _root_.gridscale.miniclust
import org.openmole.plugin.environment.gridscale.GridScaleJobService

import java.util.UUID

object MiniClustEnvironment:

  val maxConnections = PreferenceLocation("MiniclustEnvironment", "MaxConnections", Some(20))

  def apply(
    login: String,
    url: String,
    insecure: Boolean = false,
    openMOLEMemory: OptionalArgument[Information] = None,
    core: OptionalArgument[Int] = None,
    time: OptionalArgument[Time] = None,
    runtimeSetting: OptionalArgument[RuntimeSetting] = None,
    debug: Boolean = false)(using varName: sourcecode.Name, store: AuthenticationStore, pref: Preference, cypher: Cypher, replicaCatalog: ReplicaCatalog) =
    EnvironmentBuilder: ms =>
      new MiniClustEnvironment(
        url = url,
        insecure = insecure,
        openMOLEMemory = openMOLEMemory,
        core = core,
        time = time,
        name = Some(varName.value),
        authentication = MiniClustAuthentication.find(login, url),
        runtimeSetting = runtimeSetting,
        debug = debug,
        services = BatchEnvironment.Services(ms)
      )

  def toMiniclust(authentication: MiniClustAuthentication, insecure: Boolean) =
    val loginPassword =
      authentication match
        case a: MiniClustAuthentication.LoginPassword => a

    val auth = _root_.gridscale.authentication.UserPassword(loginPassword.login, loginPassword.password)

    val server =
      _root_.gridscale.miniclust.MiniclustServer(loginPassword.url, auth, insecure = insecure)

    _root_.gridscale.miniclust.Miniclust(server)


class MiniClustEnvironment(
  val url: String,
  val insecure: Boolean,
  val openMOLEMemory: Option[Information],
  val core: Option[Int],
  val time: Option[Time],
  val runtimeSetting: Option[RuntimeSetting],
  val name:              Option[String],
  val authentication: MiniClustAuthentication,
  val debug: Boolean,
  implicit val services: BatchEnvironment.Services) extends BatchEnvironment(BatchEnvironmentState(services)):

  val accessControl = AccessControl(services.preference(MiniClustEnvironment.maxConnections))
  given mc: _root_.gridscale.miniclust.Miniclust = MiniClustEnvironment.toMiniclust(authentication, insecure = insecure)

  val storage = MiniClustStorage(mc, accessControl)
  val storageSpace =
    import services.*
    AccessControl.defaultPrirority:
      HierarchicalStorageSpace.create(storage, "openmole", _ => false)

  override def execute(batchExecutionJob: BatchExecutionJob)(using Priority): BatchJobControl = accessControl:
    import services.*

    val jobDirectory = HierarchicalStorageSpace.createJobDirectory(storage, storageSpace)

    def replicate =
      BatchEnvironment.toReplicatedFile(
        upload = StorageService.uploadTimedFileInDirectory(storage, _, storageSpace.replicaDirectory, _),
        exist = StorageService.exists(storage, _),
        remove = StorageService.rmFile(storage, _),
        environment = this,
        storageId = StorageService.id(storage)
      )

    val serializedJob =
      BatchEnvironment.serializeJob(
        environment = this,
        runtimeSetting = runtimeSetting,
        job = batchExecutionJob,
        remoteStorage = MiniClustStorage.Remote(),
        replicate = replicate,
        upload = (p, t) => StorageService.uploadTimedFileInDirectory(storage, p, jobDirectory, t),
        storageId = StorageService.id(storage),
        archiveResult = true
      )

    val stdout = uniqName("out", ".txt")
    val stderr = uniqName("err", ".txt")
    val scriptName = uniqName("script", ".sh")
    val resultName = uniqName("result", ".bin")

    val runScriptPath =
      TmpDirectory.withTmpFile("script", ".sh"): script =>
        import MiniClustStorage.nodeInputPath
        val runtime =
          Seq(
            s"mkdir envplugins"
          )

        val plugins =
          for (plugin, index) <- serializedJob.runtime.environmentPlugins.zipWithIndex
          yield s"ln ${nodeInputPath(plugin.path)} envplugins/plugin$index.jar"

        val memory = BatchEnvironment.openMOLEMemoryValue(openMOLEMemory).toMegabytes.toInt

        val run =
          Seq(
            "export PATH=$PWD/jvm/jre/bin:$PATH",
            "export HOME=$PWD",
            s"""/bin/sh runtime/run.sh ${memory}m $$PWD/${UUID.randomUUID} --home-directory $$PWD -s $${PWD}/${nodeInputPath(serializedJob.remoteStorage.path)} -p $${PWD}/envplugins/ --input-file $${PWD}/${nodeInputPath(serializedJob.inputPath)} -o ${resultName}""" + (if debug then " -d 2>&1" else "")
          )

        script.content =
          (runtime ++ plugins ++ run).mkString(" && ")

        val path = s"$jobDirectory/launch.sh"
        StorageService.upload(storage, script, path, TransferOptions.default)
        path

    val inputsFiles =
      val fileMessages: Seq[FileMessage] =
        (serializedJob.executionMessage.plugins.map(_.toFileMessage) ++
          serializedJob.executionMessage.files.map(_.toFileMessage) ++
          serializedJob.runtime.environmentPlugins ++
          Seq(serializedJob.remoteStorage)).toSeq

      // TODO make sure that files are hashed using blake 3
      import _root_.miniclust.message.InputFile
      fileMessages.map: r =>
        val nodePath = MiniClustStorage.nodeInputPath(r.path)
        InputFile(r.path, nodePath, Some(s"blake3:${r.hash}"))
      ++ Seq(
        InputFile(serializedJob.inputPath, MiniClustStorage.nodeInputPath(serializedJob.inputPath)),
        InputFile(runScriptPath, scriptName)
      ) ++ Seq(
        InputFile(serializedJob.runtime.jvmLinuxX64.path, "jvm", Some(InputFile.Cache(s"blake3:${serializedJob.runtime.jvmLinuxX64.hash}", extraction = Some(InputFile.Extraction.TarGZ)))),
        InputFile(serializedJob.runtime.runtime.path, "runtime", Some(InputFile.Cache(s"blake3:${serializedJob.runtime.runtime.hash}", extraction = Some(InputFile.Extraction.TarGZ))))
      )

    val outputFiles =
      Seq(
        _root_.miniclust.message.OutputFile(resultName, resultName)
      )

    val job =
      _root_.gridscale.miniclust.submit(
        _root_.gridscale.miniclust.MinclustJobDescription(
          command = s"bash -x ${scriptName}",
          inputFile = inputsFiles,
          outputFile = outputFiles,
          stdOut = Some(stdout),
          stdErr = Some(stderr),
          resource =
            core.toSeq.map(c => _root_.gridscale.miniclust.Resource.Core(c)) ++
              time.toSeq.map(t => _root_.gridscale.miniclust.Resource.MaxTime(t.toSeconds.toInt))
        )
      )

    def downloadResult(r: String, local: File, options: TransferOptions, priority: AccessControl.Priority) =
      StorageService.download(storage, _root_.miniclust.message.MiniClust.User.jobOutputPath(job.id, r), local)

    def state = accessControl:
      GridScaleJobService.translateStatus:
        _root_.gridscale.miniclust.state(job)
    
    def delete = accessControl:
      _root_.gridscale.miniclust.cancel(job)

    def stdOutErr =accessControl:
      (_root_.gridscale.miniclust.stdOut(job).getOrElse(""), _root_.gridscale.miniclust.stdErr(job).getOrElse(""))

    def clean = accessControl:
      StorageService.rmDirectory(storage, jobDirectory)
      _root_.gridscale.miniclust.clean(job)

    BatchJobControl(
      BatchEnvironment.defaultUpdateInterval(services.preference),
      StorageService.id(storage),
      priority => state,
      priority => delete,
      priority => stdOutErr,
      downloadResult,
      resultName,
      priority => clean
    )

  override def start(): Unit = ()
  override def stop(): Unit =
    AccessControl.defaultPrirority:
      HierarchicalStorageSpace.clean(storage, storageSpace, false)
    BatchEnvironment.waitJobKilled(this)
    mc.close()

