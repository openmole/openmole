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
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class OpenMOLERESTServerAPI(fetch: CoreFetch) extends ServerAPI:
  override def size(safePath: SafePath)(using BasePath) = fetch.futureError(_.size(safePath).future)
  override def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath) = fetch.futureError(_.copyFiles(paths, overwrite).future)
  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean)(using BasePath): Future[(Boolean, String)] = fetch.futureError(_.saveFile(safePath, content, hash, overwrite).future)
  override def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean] = fetch.futureError(_.createFile(path, name, directory).future)
  override def extract(path: SafePath)(using BasePath): Future[Option[ErrorData]] = fetch.futureError(_.extract(path).future)
  override def listFiles(path: SafePath, filter: FileFilter)(using BasePath): Future[ListFilesData] = fetch.futureError(_.listFiles(path, filter).future)
  override def listRecursive(path: SafePath, findString: Option[String])(using BasePath): Future[Seq[(SafePath, Boolean)]] = fetch.futureError(_.listRecursive(path, findString).future)
  override def move(from: SafePath, to: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.move(from, to).future)
  override def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit] = fetch.futureError(_.deleteFiles(path).future)
  override def exists(path: SafePath)(using BasePath): Future[Boolean] = fetch.futureError(_.exists(path).future)
  override def temporaryDirectory()(using BasePath): Future[SafePath] = fetch.futureError(_.temporaryDirectory(()).future)
  override def executionState(line: Int, ids: Seq[ExecutionId])(using BasePath): Future[Seq[ExecutionData]] = fetch.futureError(_.executionState(line, ids).future)
  override def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit] = fetch.futureError(_.cancelExecution(id).future)
  override def removeExecution(id: ExecutionId)(using BasePath): Future[Unit] = fetch.futureError(_.removeExecution(id).future)
  override def compileScript(script: SafePath)(using BasePath): Future[Option[ErrorData]] = fetch.futureError(_.compileScript(script).future, timeout = Some(600 seconds), warningTimeout = None)
  override def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId] = fetch.futureError(_.launchScript(script, validate).future, timeout = Some(120 seconds), warningTimeout = Some(30 seconds))
  override def clearEnvironmentError(environment: EnvironmentId)(using BasePath): Future[Unit] = fetch.futureError(_.clearEnvironmentErrors(environment).future)
  override def listEnvironmentError(environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentErrorGroup]] = fetch.futureError(_.listEnvironmentErrors(environment, lines).future)
  override def listPlugins()(using BasePath): Future[Seq[Plugin]] = fetch.futureError(_.listPlugins(()).future)
  override def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]] = fetch.futureError(_.addPlugin(path).future)
  override def removePlugin(path: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.removePlugin(path).future)
  override def omrMethod(path: SafePath)(using BasePath): Future[String] = fetch.futureError(_.omrMethod(path).future)
  override def models(path: SafePath)(using BasePath): Future[Seq[SafePath]] = fetch.futureError(_.models(path).future)
  override def expandResources(resources: Resources)(using BasePath): Future[Resources] = fetch.futureError(_.expandResources(resources).future)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean)(using BasePath): Future[Unit] = fetch.futureError(_.downloadHTTP(url, path, extract, overwrite).future)
  override def marketIndex()(using BasePath): Future[MarketIndex] = fetch.futureError(_.marketIndex(()).future)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit] = fetch.futureError(_.getMarketEntry(entry, safePath).future)
  override def omSettings()(using BasePath): Future[OMSettings] = fetch.futureError(_.omSettings(()).future)
  override def shutdown()(using BasePath): Future[Unit] = fetch.futureError(_.shutdown(()).future)
  override def restart()(using BasePath): Future[Unit] = fetch.futureError(_.restart(()).future)
  override def isAlive()(using BasePath): Future[Boolean] = fetch.futureError(_.isAlive(()).future, timeout = Some(3 seconds), warningTimeout = None)
  override def jvmInfos()(using BasePath): Future[JVMInfos] = fetch.futureError(_.jvmInfos(()).future)
  override def mdToHtml(safePath: SafePath)(using BasePath): Future[String] = fetch.futureError(_.mdToHtml(safePath).future)
  override def sequence(safePath: SafePath)(using BasePath): Future[SequenceData] = fetch.futureError(_.sequence(safePath).future)
  override def listNotification()(using BasePath): Future[Seq[NotificationEvent]] = fetch.futureError(_.listNotification(()).future)
  override def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit] = fetch.futureError(_.clearNotification(ids).future)

  override def upload(
    fileList: FileList,
    destinationPath: SafePath,
    fileTransferState: ProcessState ⇒ Unit,
    onLoadEnd: Seq[String] ⇒ Unit)(using basePath: BasePath): Unit =
    val formData = new FormData

    formData.append("fileType", destinationPath.context.typeName)

    for (i ← 0 to fileList.length - 1) {
      val file = fileList(i)
      formData.append(Utils.toURI(destinationPath.path ++ Seq(file.name)), file)
    }

    val xhr = new XMLHttpRequest

    xhr.upload.onprogress = e ⇒ {
      fileTransferState(Processing((e.loaded.toDouble * 100 / e.total).toInt))
    }

    xhr.upload.onloadend = e ⇒ {
      fileTransferState(Finalizing())
    }

    xhr.onloadend = e ⇒ {
      fileTransferState(Processed())
      onLoadEnd(fileList.map(_.name).toSeq)
    }

    val prefix = BasePath.value(basePath).getOrElse("")

    xhr.open("POST", org.openmole.gui.shared.data.uploadFilesRoute, true)
    xhr.send(formData)


  override def download(
    safePath: SafePath,
    fileTransferState: ProcessState ⇒ Unit = _ ⇒ (),
    onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (),
    hash: Boolean = false)(using basePath: BasePath): Unit =
    size(safePath).foreach { size ⇒
      val xhr = new XMLHttpRequest

      xhr.onprogress = (e: ProgressEvent) ⇒ {
        fileTransferState(Processing((e.loaded.toDouble * 100 / size).toInt))
      }

      xhr.onloadend = (e: ProgressEvent) ⇒ {
        fileTransferState(Processed())
        val h = Option(xhr.getResponseHeader(hashHeader))
        onLoadEnd(xhr.responseText, h)
      }

      xhr.open("GET", downloadFile(Utils.toURI(safePath.path.map { Encoding.encode }), hash = hash), true)
      xhr.send()
    }

  override def fetchGUIPlugins(f: GUIPlugins ⇒ Unit)(using BasePath) =
    fetch.futureError(_.guiPlugins(()).future).map { p ⇒
      val authFact = p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
      val wizardFactories = p.wizards.map { gp ⇒ Plugins.buildJSObject[WizardPluginFactory](gp) }
      val analysisFactories = p.analysis.map { (method, gp) ⇒ (method, Plugins.buildJSObject[MethodAnalysisPlugin](gp)) }.toMap
      f(GUIPlugins(authFact, wizardFactories, analysisFactories))
    }