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

class OpenMOLERESTServerAPI(fetch: Fetch) extends ServerAPI:
  override def size(safePath: SafePath) = fetch.future(_.size(safePath).future)
  override def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = fetch.future(_.copyFiles(safePaths, to, overwrite).future)
  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)] = fetch.future(_.saveFile(safePath, content, hash, overwrite).future)
  override def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean] = fetch.future(_.createFile(path, name, directory).future)
  override def extract(path: SafePath): Future[Option[ErrorData]] = fetch.future(_.extract(path).future)
  override def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData] = fetch.future(_.listFiles(path, filter).future)
  override def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] = fetch.future(_.listRecursive(path, findString).future)
  override def move(from: SafePath, to: SafePath): Future[Unit] = fetch.future(_.move(from, to).future)
  override def duplicate(path: SafePath, name: String): Future[SafePath] = fetch.future(_.duplicate(path, name).future)
  override def deleteFiles(path: Seq[SafePath]): Future[Unit] = fetch.future(_.deleteFiles(path).future)
  override def exists(path: SafePath): Future[Boolean] = fetch.future(_.exists(path).future)
  override def temporaryDirectory(): Future[SafePath] = fetch.future(_.temporaryDirectory(()).future)
  override def allStates(line: Int): Future[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = fetch.future(_.allStates(line).future)
  override def staticInfos(): Future[Seq[(ExecutionId, StaticExecutionInfo)]] = fetch.future(_.staticInfos(()).future)
  override def cancelExecution(id: ExecutionId): Future[Unit] = fetch.future(_.cancelExecution(id).future)
  override def removeExecution(id: ExecutionId): Future[Unit] = fetch.future(_.removeExecution(id).future)
  override def compileScript(script: SafePath): Future[Option[ErrorData]] = fetch.future(_.compileScript(script).future, timeout = 120 seconds, warningTimeout = 60 seconds)
  override def runScript(script: SafePath, validate: Boolean): Future[Unit] = fetch.future(_.runScript(script, validate).future, timeout = 120 seconds, warningTimeout = 60 seconds)
  override def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit] = fetch.future(_.clearEnvironmentErrors(environment).future)
  override def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData] = fetch.future(_.runningErrorEnvironmentData(environment, lines).future)
  override def listPlugins(): Future[Seq[Plugin]] = fetch.future(_.listPlugins(()).future)
  override def addPlugin(path: SafePath): Future[Seq[ErrorData]] = fetch.future(_.addPlugin(path).future)
  override def removePlugin(path: SafePath): Future[Unit] = fetch.future(_.removePlugin(path).future)
  override def omrMethod(path: SafePath): Future[String] = fetch.future(_.omrMethod(path).future)
  override def models(path: SafePath): Future[Seq[SafePath]] = fetch.future(_.models(path).future)
  override def expandResources(resources: Resources): Future[Resources] = fetch.future(_.expandResources(resources).future)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean): Future[Option[ErrorData]] = fetch.future(_.downloadHTTP(url, path, extract).future)
  override def marketIndex(): Future[MarketIndex] = fetch.future(_.marketIndex(()).future)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Future[Unit] = fetch.future(_.getMarketEntry(entry, safePath).future)
  override def omSettings(): Future[OMSettings] = fetch.future(_.omSettings(()).future)
  override def shutdown(): Future[Unit] = fetch.future(_.shutdown(()).future)
  override def restart(): Future[Unit] = fetch.future(_.restart(()).future)
  override def isAlive(): Future[Boolean] = fetch.future(_.isAlive(()).future, 3.seconds, 5.minutes)
  override def jvmInfos(): Future[JVMInfos] = fetch.future(_.jvmInfos(()).future)
  override def mdToHtml(safePath: SafePath): Future[String] = fetch.future(_.mdToHtml(safePath).future)
  override def sequence(safePath: SafePath): Future[SequenceData] = fetch.future(_.sequence(safePath).future)

  override def upload(
    fileList: FileList,
    destinationPath: SafePath,
    fileTransferState: ProcessState ⇒ Unit,
    onLoadEnd: Seq[String] ⇒ Unit): Unit =
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

    xhr.open("POST", org.openmole.gui.shared.data.uploadFilesRoute, true)
    xhr.send(formData)


  override def download(
    safePath: SafePath,
    fileTransferState: ProcessState ⇒ Unit = _ ⇒ (),
    onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (),
    hash: Boolean = false): Unit =
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

      xhr.open("GET", downloadFile(Utils.toURI(safePath.path.map {
        Encoding.encode
      }), hash = hash), true)
      xhr.send()
    }

  override def fetchGUIPlugins(f: GUIPlugins ⇒ Unit) =
    fetch.future(_.guiPlugins(()).future).map { p ⇒
      val authFact = p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
      val wizardFactories = p.wizards.map { gp ⇒ Plugins.buildJSObject[WizardPluginFactory](gp) }
      val analysisFactories = p.analysis.map { (method, gp) ⇒ (method, Plugins.buildJSObject[MethodAnalysisPlugin](gp)) }.toMap
      f(GUIPlugins(authFact, wizardFactories, analysisFactories))
    }