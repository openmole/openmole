package org.openmole.gui.client.ext

import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.shared.data.*
import org.scalajs.dom
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


object BasePath:
  def apply(location: dom.Location): BasePath =
    val basePath = location.pathname.split("/").dropRight(1).mkString("/")
    if basePath.isEmpty then None else Some(basePath)
  implicit def value(p: BasePath): Option[String] = p

opaque type BasePath = Option[String]

trait ServerAPI:
  def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath): Future[Seq[SafePath]]
  def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean)(using BasePath): Future[(Boolean, String)]
  def size(safePath: SafePath)(using BasePath): Future[Long]
  def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean]
  def extract(path: SafePath)(using BasePath): Future[Option[ErrorData]]
  def listFiles(path: SafePath, filter: FileFilter)(using BasePath): Future[ListFilesData]
  def listRecursive(path: SafePath, findString: Option[String])(using BasePath): Future[Seq[(SafePath, Boolean)]]
  def move(from: SafePath, to: SafePath)(using BasePath): Future[Unit]
  def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit]
  def exists(path: SafePath)(using BasePath): Future[Boolean]
  def temporaryDirectory()(using BasePath): Future[SafePath]

  def executionState(line: Int, id: Seq[ExecutionId] = Seq())(using BasePath): Future[Seq[ExecutionData]]
  def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit]
  def removeExecution(id: ExecutionId)(using BasePath): Future[Unit]
  def compileScript(script: SafePath)(using BasePath): Future[Option[ErrorData]]
  def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId]
  def clearEnvironmentError(environment: EnvironmentId)(using BasePath): Future[Unit]
  def listEnvironmentError(environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentErrorGroup]]

  def listPlugins()(using BasePath): Future[Seq[Plugin]]
  def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]]
  def removePlugin(path: SafePath)(using BasePath): Future[Unit]

  def omrMethod(path: SafePath)(using BasePath): Future[String]
  def fetchGUIPlugins(f: GUIPlugins ⇒ Unit)(using BasePath): Future[Unit]

  def models(path: SafePath)(using BasePath): Future[Seq[SafePath]]
  def expandResources(resources: Resources)(using BasePath): Future[Resources]
  def downloadHTTP(url: String, path: SafePath, extract: Boolean)(using BasePath): Future[Option[ErrorData]]

  def marketIndex()(using BasePath): Future[MarketIndex]
  def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit]

  def omSettings()(using BasePath): Future[OMSettings]
  def shutdown()(using BasePath): Future[Unit]
  def restart()(using BasePath): Future[Unit]
  def isAlive()(using BasePath): Future[Boolean]
  def jvmInfos()(using BasePath): Future[JVMInfos]
  def listNotification()(using BasePath): Future[Seq[NotificationEvent]]
  def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit]

  def mdToHtml(safePath: SafePath)(using BasePath): Future[String]
  def sequence(safePath: SafePath)(using BasePath): Future[SequenceData]

  def upload(fileList: FileList, destinationPath: SafePath, fileTransferState: ProcessState ⇒ Unit, onLoadEnd: Seq[String] ⇒ Unit)(using BasePath): Unit
  def download(safePath: SafePath, fileTransferState: ProcessState ⇒ Unit = _ ⇒ (), onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (), hash: Boolean = false)(using BasePath): Unit