/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.runtime.daemon

import java.io.File
import java.util.Random
import java.util.UUID
import java.util.concurrent.Executors
import fr.iscpif.gridscale.authentication.UserPassword
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.plugin.environment.gridscale._
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.thread._
import org.openmole.core.tools.service.{ OS, ProcessUtil }
import org.openmole.core.workspace.{ Workspace, ConfigurationLocation }
import org.openmole.plugin.environment.desktopgrid._
import DesktopGridEnvironment._
import org.openmole.core.batch.message._
import org.openmole.core.batch.storage._
import org.openmole.core.serializer._
import org.openmole.tool.tar._
import org.openmole.tool.hash._
import ProcessUtil._
import fr.iscpif.gridscale.ssh._

import org.openmole.core.batch.message.FileMessage._
import scala.annotation.tailrec
import util.{ Failure, Success }

object JobLauncher extends Logger {
  val jobCheckInterval = new ConfigurationLocation("JobLauncher", "jobCheckInterval")
  val connectionTimeout = new ConfigurationLocation("JobLauncher", "connectionTimeout")

  Workspace setDefault (jobCheckInterval, "PT1M")
  Workspace setDefault (connectionTimeout, "PT1M")
}

class JobLauncher(cacheSize: Long, debug: Boolean) {
  import JobLauncher._
  import Log._

  val localCache = new FileCache {
    val limit = cacheSize
  }

  val resultUploader = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory)

  def launch(userHostPort: String, password: String, nbWorkers: Int) = {
    val host = userHostPort.split("@").last
    val splitHost = host.split(":")
    val _port = if (splitHost.size == 2) splitHost(1).toInt else 22
    val _host = splitHost(0)

    logger.info(s"Looking for jobs on ${_host} port ${_port}")

    val storage = new SimpleStorage with GridScaleStorage with CompressedTransfer {

      val storage = new SSHStorage {
        val host = _host
        override val port = _port
        def credential = UserPassword("", password)
        def timeout = Workspace.preferenceAsDuration(connectionTimeout)
      }

      val root = ""
    }

    val storageFile = Workspace.newFile()
    SerialiserService.serialiseAndArchiveFiles(new LocalSimpleStorage, storageFile)

    (0 until nbWorkers).foreach {
      i ⇒ background { runJobs(storageFile, storage) }
    }

    Thread.currentThread.join
  }

  def runJobs(storageFile: File, storage: SimpleStorage) = {
    val id = UUID.randomUUID
    implicit val rng = new Random(id.getLeastSignificantBits ^ id.getMostSignificantBits)

    processJob(background { fetchAJob(id, storage) })

    @tailrec def processJob(fetchJobResult: () ⇒ Option[(File, File, File, File, Int, ExecutionMessage, String, List[FileMessage])]): Unit = {
      val next = try {
        fetchJobResult() match {

          case Some((localExecutionMessage, localCommunicationDirPath, runtime, pluginDir, memory, executionMessage, job, cached)) ⇒
            val next = background { fetchAJob(id, storage) }

            val localResultFile =
              try {
                val localResultFile = Workspace.newFile("job", ".res")
                val configurationDir = Workspace.newDir("configuration")
                val workspaceDir = Workspace.newDir("workspace")
                val osgiDir = new File(runtime, UUID.randomUUID.toString)

                def quote(s: String) = if (OS.actualOS.isWindows) '"' + s + '"' else s.replace(" ", "\\ ")
                val ptrFlag = if (OS.actualOS.is64Bit) "-XX:+UseCompressedOops" else ""

                val cmd = s"java -Xmx${memory}m -Dosgi.locking=none -XX:+UseG1GC $ptrFlag -Dosgi.configuration.area=${osgiDir.getName} -Dosgi.classloader.singleThreadLoads=true -jar plugins/org.eclipse.equinox.launcher.jar -configuration ${quote(configurationDir.getAbsolutePath)} -s ${quote(storageFile.getAbsolutePath)} -i ${quote(localExecutionMessage.getAbsolutePath)} -o ${quote(localResultFile.getAbsolutePath)} -c ${localCommunicationDirPath} -p ${quote(pluginDir.getAbsolutePath)}" + (if (debug) " -d " else "")

                logger.info("Executing runtime: " + cmd)
                //val commandLine = CommandLine.parse(cmd)
                val process = Runtime.getRuntime.exec(cmd, Array("OPENMOLE_HOME=" + workspaceDir.getAbsolutePath), runtime) //commandLine.toString, null, runtimeLocation)
                executeProcess(process, System.out, System.err)
                logger.info("Process finished.")

                configurationDir.recursiveDelete
                workspaceDir.recursiveDelete
                osgiDir.recursiveDelete
                pluginDir.recursiveDelete
                localExecutionMessage.delete
                localResultFile
              }
              finally {
                localCache.release(cached)
              }

            resultUploader.submit({
              try {
                uploadResult(localResultFile, executionMessage.communicationDirPath, job, storage)
                localCommunicationDirPath.recursiveDelete
                localResultFile.delete
              }
              catch {
                case e: Throwable ⇒ logger.log(WARNING, "Error during result upload", e)
              }
            })

            //if(ret != 0) throw new InternalProcessingError("Error executing: " + commandLine +" return code was not 0 but " + ret)
            next
          case None ⇒
            logger.info("Job list is empty on the remote host.")
            Thread.sleep(Workspace.preferenceAsDuration(jobCheckInterval).toMillis)
            background { fetchAJob(id, storage) }
        }
      }
      catch {
        case e: Exception ⇒
          logger.log(WARNING, s"Error while looking for jobs, it might happen if the jobs have not yep been made on the server side. Automatic retry in ${Workspace.preferenceAsDuration(jobCheckInterval)}.", e)
          Thread.sleep(Workspace.preferenceAsDuration(jobCheckInterval).toMillis)
          background { fetchAJob(id, storage) }
      }
      processJob(next)
    }
  }

  def uploadResult(localResultFile: File, communicationDir: String, job: String, storage: SimpleStorage) = {
    val runtimeResult = SerialiserService.deserialiseAndExtractFiles[RuntimeResult](localResultFile)

    logger.info(s"Uploading context results to communication dir $communicationDir")

    def uploadFileMessage(msg: FileMessage) = {
      val localFile = new File(msg.path)
      val uploadedFile = storage.child(communicationDir, Storage.uniqName("fileMsg", ".bin"))
      logger.info("Uploading " + localFile + " to " + uploadedFile)
      try storage.upload(localFile, uploadedFile)
      finally localFile.delete
      logger.info("Uploaded " + localFile)
      FileMessage(uploadedFile, msg.hash)
    }

    def uploadReplicatedFile(replicated: ReplicatedFile): ReplicatedFile = {
      val FileMessage(uploaded, _) = uploadFileMessage(replicated)
      replicated.copy(path = uploaded)
    }

    val uploadedResult = runtimeResult.result match {
      case Success((ArchiveContextResults(contextResults), log)) ⇒
        Success((ArchiveContextResults(contextResults), log))
      case Success((IndividualFilesContextResults(contextResults, files: Iterable[ReplicatedFile]), log)) ⇒
        val uploadedFiles = files.map { uploadReplicatedFile }
        Success((IndividualFilesContextResults(contextResults, uploadedFiles), log))
      case Failure(e) ⇒ Failure(e)
    }

    logger.info("Context results uploaded")
    val resultToSend = runtimeResult.copy(result = uploadedResult)

    // Upload the result
    Workspace.withTmpFile { outputLocal ⇒
      logger.info("Uploading job results")
      SerialiserService.serialiseAndArchiveFiles(resultToSend, outputLocal)
      val tmpResultFile = storage.child(tmpResultsDirName, Storage.uniqName(job, ".res"))
      storage.upload(outputLocal, tmpResultFile)
      val resultFile = storage.child(resultsDirName, Storage.uniqName(job, ".res"))
      storage.mv(tmpResultFile, resultFile)
      logger.info("Job results uploaded at " + resultFile)
    }

  }

  def selectAJob(id: UUID, storage: SimpleStorage)(implicit rng: Random) = {
    val jobs = storage.listNames(jobsDirName)

    if (!jobs.isEmpty) {
      val timeStemps = storage.listNames(timeStempsDirName)

      val groupedStemps = timeStemps.map { ts ⇒ ts.split(timeStempSeparator).head → ts }.groupBy { _._1 }
      val stempsByJob = jobs.map { j ⇒ j → groupedStemps.getOrElse(j, Iterable.empty).map { _._2 } }

      val possibleChoices = stempsByJob.map { case (j, s) ⇒ s.size → j }.foldLeft(Int.MaxValue → List.empty[String]) {
        (acc, cur) ⇒
          if (cur._1 < acc._1) cur._1 → List(cur._2)
          else if (cur._1 > acc._1) acc
          else acc._1 → (cur._2 +: acc._2)
      }

      logger.info("Choose between " + possibleChoices._2.size + " jobs with " + possibleChoices._1 + " timestemps ")

      val index = rng.nextInt(possibleChoices._2.size)
      val job = possibleChoices._2(index)

      /*val job = if (possibleChoices._1 == 0) {
        logger.info("Choose a job at random")
        val index = rng.nextInt(possibleChoices._2.size)
        possibleChoices._2(index)
      } else {
        logger.info("Choose a job with older timestemp")

        def olderTimeStemp(job: String) = groupedStemps(job).map { v ⇒ timeStempsDir.modificationTime(v._2) }.min

        possibleChoices._2.map { job ⇒ olderTimeStemp(job) -> job }.min(Ordering.by { j: (Long, _) ⇒ j._1 })._2
      }*/

      logger.info("Choosen job is " + job)
      storage.create(storage.child(timeStempsDirName, job + timeStempSeparator + UUID.randomUUID))

      val jobMessage =
        Workspace.withTmpFile {
          f ⇒
            storage.download(storage.child(jobsDirName, job), f)
            SerialiserService.deserialise[DesktopGridJobMessage](f)
        }

      logger.info("Job execution message is " + jobMessage.executionMessagePath)
      Some(job → jobMessage)
    }
    else None
  }

  def fetchAJob(id: UUID, storage: SimpleStorage)(implicit rng: Random) = {

    def download(fileMessage: FileMessage, raw: Boolean) = {
      val file = Workspace.newFile("cache", ".bin")
      storage.download(fileMessage.path, file, TransferOptions(raw = raw))
      file → fileMessage.hash
    }

    selectAJob(id, storage) match {
      case Some((job, jobMessage)) ⇒
        var cached = List.empty[FileMessage]

        try {
          val runtime =
            localCache.cache(
              jobMessage.runtime,
              msg ⇒ {
                val dir = Workspace.newDir()
                logger.info("Downloading the runtime.")
                val (archive, hash) = download(msg, true)
                logger.info("Extracting runtime.")
                archive.extractUncompress(dir)
                dir → hash
              }
            )
          cached ::= jobMessage.runtime

          val pluginDir = Workspace.newDir()
          pluginDir.mkdirs

          jobMessage.runtimePlugins.foreach {
            fileMessage ⇒
              val plugin = localCache.cache(fileMessage, download(_, true))
              cached ::= fileMessage
              plugin.copy(File.createTempFile("plugin", ".jar", pluginDir))
          }

          val executionMessage = Workspace.withTmpFile { executionMessageFileCache ⇒
            storage.download(jobMessage.executionMessagePath, executionMessageFileCache)
            SerialiserService.deserialiseAndExtractFiles[ExecutionMessage](executionMessageFileCache)
          }

          def localCachedReplicatedFile(replicatedFile: ReplicatedFile, raw: Boolean) = {
            val localFile = localCache.cache(replicatedFile, download(_, raw))
            cached ::= replicatedFile
            replicatedFile.copy(path = localFile.getAbsolutePath)
          }

          val files = executionMessage.files.map(localCachedReplicatedFile(_, raw = false))
          val plugins = executionMessage.plugins.map(localCachedReplicatedFile(_, raw = true))

          val localCommunicationDirPath = Workspace.newDir()
          localCommunicationDirPath.mkdirs

          val localExecutionMessage = Workspace.newFile("executionMessage", ".gz")

          SerialiserService.serialiseAndArchiveFiles(ExecutionMessage(plugins, files, executionMessage.jobs, localCommunicationDirPath.getAbsolutePath, executionMessage.runtimeSettings), localExecutionMessage)

          Some((localExecutionMessage, localCommunicationDirPath, runtime, pluginDir, jobMessage.memory, executionMessage, job, cached))
        }
        catch {
          case e: Throwable ⇒
            localCache.release(cached)
            throw e
        }
      case None ⇒ None
    }
  }

}
