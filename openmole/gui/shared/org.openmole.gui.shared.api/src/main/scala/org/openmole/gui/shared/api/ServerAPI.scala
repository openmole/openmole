package org.openmole.gui.shared.api

import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.shared.data.*
import org.scalajs.dom.FileList

import scala.concurrent.Future

/*
 * Copyright (C) 2023 Romain Reuillon
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

trait ServerAPI:
  def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean): Future[Seq[SafePath]]
  def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)]
  def size(safePath: SafePath): Future[Long]
  def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean]
  def extract(path: SafePath): Future[Option[ErrorData]]
  def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData]
  def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]]
  def move(from: SafePath, to: SafePath): Future[Unit]
  def duplicate(path: SafePath, name: String): Future[SafePath]
  def deleteFiles(path: Seq[SafePath]): Future[Unit]
  def exists(path: SafePath): Future[Boolean]
  def temporaryDirectory(): Future[SafePath]

  def allStates(line: Int): Future[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])]
  def staticInfos(): Future[Seq[(ExecutionId, StaticExecutionInfo)]]
  def cancelExecution(id: ExecutionId): Future[Unit]
  def removeExecution(id: ExecutionId): Future[Unit]
  def compileScript(script: ScriptData): Future[Option[ErrorData]]
  def runScript(script: ScriptData, validate: Boolean): Future[Unit]
  def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit]
  def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData]

  def listPlugins(): Future[Seq[Plugin]]
  def guiPlugins(): Future[PluginExtensionData]
  def addPlugin(path: SafePath): Future[Seq[ErrorData]]
  def removePlugin(path: SafePath): Future[Unit]
  def findVisualisationPlugin(path: SafePath): Future[Option[GUIPluginAsJS]]

  def models(path: SafePath): Future[Seq[SafePath]]
  def expandResources(resources: Resources): Future[Resources]
  def downloadHTTP(url: String, path: SafePath, extract: Boolean): Future[Option[ErrorData]]

  def marketIndex(): Future[MarketIndex]
  def  getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Future[Unit]

  def omSettings(): Future[OMSettings]
  def shutdown(): Future[Unit]
  def restart(): Future[Unit]
  def isAlive(): Future[Boolean]
  def jvmInfos(): Future[JVMInfos]

  def mdToHtml(safePath: SafePath): Future[String]
  def sequence(safePath: SafePath): Future[SequenceData]

  def upload(fileList: FileList, destinationPath: SafePath, fileTransferState: ProcessState ⇒ Unit, onLoadEnd: Seq[String] ⇒ Unit): Unit
  def download(safePath: SafePath, fileTransferState: ProcessState ⇒ Unit = _ ⇒ (), onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (), hash: Boolean = false): Unit