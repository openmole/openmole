package org.openmole.gui.client.core

/*
 * Copyright (C) 2022 Romain Reuillon
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

import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.client.core.NotificationManager.toService
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.ext.wizard.*
import org.openmole.gui.shared.api.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.io.IOException
import scala.util.Failure
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.git.GitPrivateKeyAuthenticationFactory

class OpenMOLERESTServerAPI(sttp: STTPInterpreter, notificationService: NotificationService) extends ServerAPI:
  api =>

  override def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath) = sttp.toRequest(CoreAPI.copyFiles)(paths, overwrite)

  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean)(using BasePath): Future[(Boolean, String)] =
    import scala.scalajs.js.timers.*

    val f = sttp.toRequest(CoreAPI.saveFile)(safePath, content, hash, overwrite)

    val timeoutHandle = setTimeout(60000):
      notificationService.notify(NotificationLevel.Info, s"Saving the file ${safePath.name} takes too long, you may be offline.", ClientUtil.errorTextArea(s"File save too long"))

    f.onComplete { _ => clearTimeout(timeoutHandle) }

    f

  override def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean] = sttp.toRequest(CoreAPI.createFile)(path, name, directory)
  override def extractArchive(path: SafePath, to: SafePath)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.extractArchive)(path, to)
  override def listFiles(path: SafePath, filter: FileSorting, withHidden: Boolean)(using BasePath): Future[FileListData] = sttp.toRequest(CoreAPI.listFiles)(path, filter, withHidden)
  override def listRecursive(path: SafePath, findString: Option[String], withHidden: Boolean)(using BasePath): Future[Seq[(SafePath, Boolean)]] = sttp.toRequest(CoreAPI.listRecursive)(path, findString, withHidden)
  override def move(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath) = sttp.toRequest(CoreAPI.move)(paths, overwrite)
  override def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.deleteFiles)(path)
  override def exists(path: SafePath)(using BasePath): Future[Boolean] = sttp.toRequest(CoreAPI.exists)(path)
  override def isTextFile(path: SafePath)(using BasePath): Future[Boolean] = sttp.toRequest(CoreAPI.isText)(path)
  override def temporaryDirectory()(using BasePath): Future[SafePath] = sttp.toRequest(CoreAPI.temporaryDirectory)(())
  override def executionState(ids: Seq[ExecutionId])(using BasePath): Future[Seq[ExecutionData]] = sttp.toRequest(CoreAPI.executionState)(ids)
  override def executionOutput(id: ExecutionId, lines: Int)(using BasePath): Future[ExecutionOutput] = sttp.toRequest(CoreAPI.executionOutput)(id, lines)
  override def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.cancelExecution)(id)
  override def removeExecution(id: ExecutionId)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.removeExecution)(id)
  override def validateScript(script: SafePath)(using BasePath): Future[Option[ErrorData]] = sttp.toRequest(CoreAPI.validateScript)(script) //.future, timeout = Some(600 seconds), warningTimeout = None)
  override def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId] = sttp.toRequest(CoreAPI.launchScript)(script, validate) //.future, timeout = Some(600 seconds), warningTimeout = Some(300 seconds))
  override def clearEnvironmentError(executionId: ExecutionId, environment: EnvironmentId)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.clearEnvironmentErrors)(executionId, environment)
  override def listEnvironmentError(executionId: ExecutionId, environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentError]] = sttp.toRequest(CoreAPI.listEnvironmentErrors)(executionId, environment, lines)
  override def listPlugins()(using BasePath): Future[Seq[Plugin]] = sttp.toRequest(CoreAPI.listPlugins)(())
  override def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]] = sttp.toRequest(CoreAPI.addPlugin)(path)
  override def removePlugin(path: SafePath)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.removePlugin)(path)

  override def omrMethod(path: SafePath)(using BasePath): Future[Option[String]] = sttp.toRequest(CoreAPI.omrMethod)(path)
  override def omrContent(path: SafePath, data: Option[String] = None)(using BasePath): Future[GUIOMRContent] = sttp.toRequest(CoreAPI.omrContent)((path, data))
  override def omrFiles(path: SafePath)(using BasePath): Future[Option[SafePath]] = sttp.toRequest(CoreAPI.omrFiles)(path)
  override def omrDataIndex(path: SafePath)(using BasePath): Future[Seq[GUIOMRDataIndex]] = sttp.toRequest(CoreAPI.omrDataIndex)(path)

  override def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.downloadHTTP)(url, path, extract, overwrite) //.future, timeout = Some(600 seconds), warningTimeout = None)
  override def marketIndex()(using BasePath): Future[MarketIndex] = sttp.toRequest(CoreAPI.marketIndex)(())
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.getMarketEntry)(entry, safePath)

  override def omSettings()(using BasePath): Future[OMSettings] = sttp.toRequest(CoreAPI.omSettings)(())
  override def shutdown()(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.shutdown)(())

  override def jvmInfos()(using BasePath): Future[JVMInfos] = sttp.toRequest(CoreAPI.jvmInfos)(())
  override def mdToHtml(safePath: SafePath)(using BasePath): Future[String] = sttp.toRequest(CoreAPI.mdToHtml)(safePath)
  override def sequence(safePath: SafePath)(using BasePath): Future[SequenceData] = sttp.toRequest(CoreAPI.sequence)(safePath)
  override def listNotification()(using BasePath): Future[Seq[NotificationEvent]] = sttp.toRequest(CoreAPI.listNotification)(())
  override def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.clearNotification)(ids)
  override def removeContainerCache()(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.removeContainerCache)(())

  override def cloneRepository(repositoryURL: String, destination: SafePath, overwrite: Boolean)(using BasePath) = sttp.toRequest(CoreAPI.cloneRepository)(repositoryURL, destination, overwrite)
  override def commitFiles(files: Seq[SafePath], message: String)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.commit)(files, message)
  override def revertFiles(files: Seq[SafePath])(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.revert)(files)
  override def addFiles(files: Seq[SafePath])(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.add)(files)
  override def pull(from: SafePath)(using BasePath): Future[MergeStatus] = sttp.toRequest(CoreAPI.pull)(from)
  override def push(from: SafePath)(using BasePath): Future[PushStatus] = sttp.toRequest(CoreAPI.push)(from)
  override def branchList(from: SafePath)(using BasePath): Future[Option[BranchData]] = sttp.toRequest(CoreAPI.branchList)(from)
  override def checkout(from: SafePath, branchName: String)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.checkout)(from,branchName)
  override def stash(from: SafePath)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.stash)(from)
  override def stashPop(from: SafePath)(using BasePath): Future[MergeStatus] = sttp.toRequest(CoreAPI.stashPop)(from)

  override def gitAuthentications()(using BasePath): Future[Seq[GitPrivateKeyAuthenticationData]] = sttp.toRequest(CoreAPI.gitAuthentications)(())
  override def addGitAuthentication(data: GitPrivateKeyAuthenticationData)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.addGitAuthentication)(data)
  override def removeGitAuthentication(data: GitPrivateKeyAuthenticationData, delete: Boolean)(using BasePath): Future[Unit] = sttp.toRequest(CoreAPI.removeGitAuthentication)(data, delete)
  override def testGitAuthentication(data: GitPrivateKeyAuthenticationData)(using BasePath): Future[Seq[Test]] = sttp.toRequest(CoreAPI.testGitAuthentication)(data)

  def size(safePath: SafePath)(using BasePath) = sttp.toRequest(CoreAPI.size)(safePath)

  override def upload(
    files: Seq[(File, SafePath)],
    fileTransferState: ProcessState => Unit)(using basePath: BasePath): Future[Seq[(RelativePath, SafePath)]] = {
    val formData = new FormData

    val destinationPaths = files.unzip._2

    formData.append(org.openmole.gui.shared.api.fileTypeParam, destinationPaths.map(_.context.typeName).mkString(","))

    for
      (file, destination) <- files
    do
      formData.append(Util.toURI(destination.path.value), file)

    val xhr = new XMLHttpRequest

    xhr.upload.onprogress = e =>
      fileTransferState(Processing((e.loaded.toDouble * 100 / e.total).toInt))

    xhr.upload.onloadend = e =>
      fileTransferState(Finalizing())

    xhr.onloadend = e =>
      fileTransferState(Processed())

    val p = scala.concurrent.Promise[Seq[(RelativePath, SafePath)]]()

    xhr.onload = e =>
      xhr.status match
        case s if s < 300 => p.success(files.map(_._1.path) zip destinationPaths)
        case s => p.failure(new IOException(s"Upload of files ${files} failed with status $s"))

    xhr.onerror = e =>
      p.failure(new IOException(s"Upload of files ${files} failed"))

    xhr.onabort = e =>
      p.failure(new IOException(s"Upload of file ${files} was aborted"))

    xhr.ontimeout = e =>
      p.failure(new IOException(s"Upload of file ${files} timed out"))


    xhr.open("POST", org.openmole.gui.shared.api.uploadFilesRoute, true)
    xhr.send(formData)

    p.future
  }.andThen {
    case Failure(t) => notificationService.notify(NotificationLevel.Error, s"Error while uploading file", div(ErrorData.stackTrace(ErrorData(t))))
  }

  override def download(
    safePath: SafePath,
    fileTransferState: ProcessState => Unit = _ => (),
    hash: Boolean = false)(using basePath: BasePath): Future[(String, Option[String])] =
    size(safePath).flatMap { size =>
      val xhr = new XMLHttpRequest

      xhr.onprogress = (e: ProgressEvent) =>
        fileTransferState(Processing((e.loaded * 100 / size).toInt))

      xhr.onloadend = e =>
        fileTransferState(Processed())

      val p = scala.concurrent.Promise[(String, Option[String])]()

      xhr.onload = e =>
        val h = Option(xhr.getResponseHeader(hashHeader))
        xhr.status match
          case s if s < 300 => p.success((xhr.responseText, h))
          case s => p.failure(new IOException(s"Download of file ${safePath} failed with error ${s}"))

      xhr.onerror = e =>
        p.failure(new IOException(s"Download of file ${safePath} failed"))

      xhr.onabort = e =>
        p.failure(new IOException(s"Download of file ${safePath} was aborted"))

      xhr.ontimeout = e =>
        p.failure(new IOException(s"Download of file ${safePath} timed out"))


      xhr.open("GET", downloadFile(safePath, hash = hash), true)
      xhr.send()

      p.future
    }.andThen {
      case Failure(t) => notificationService.notify(NotificationLevel.Error, s"Error while downloading file", div(ErrorData.stackTrace(ErrorData(t))))
    }

  override def fetchGUIPlugins(f: GUIPlugins => Unit)(using BasePath) =
    def successOrNotify[T](t: util.Try[T]) =
      t match
        case util.Success(r) => Some(r)
        case util.Failure(t) =>
          notificationService.notify(NotificationLevel.Error, s"Error while instantiating plugin", div(ErrorData.stackTrace(ErrorData(t))))
          None

    sttp.toRequest(CoreAPI.guiPlugins)(()).map: p =>
      val authFact =
        p.authentications.flatMap { gp => successOrNotify(Plugins.buildJSObject[AuthenticationPluginFactory](gp)) } ++
          Seq(new GitPrivateKeyAuthenticationFactory(api))

      val wizardFactories = p.wizards.flatMap { gp => successOrNotify(Plugins.buildJSObject[WizardPluginFactory](gp)) }
      val analysisFactories = p.analysis.flatMap { (method, gp) => successOrNotify(Plugins.buildJSObject[MethodAnalysisPlugin](gp)).map(p => (method, p)) }.toMap
      f(GUIPlugins(authFact, wizardFactories, analysisFactories))