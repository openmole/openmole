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
  def apply(path: Option[String]): BasePath = path

  implicit def value(p: BasePath): Option[String] = p

opaque type BasePath = Option[String]

trait ServerAPI:
  def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath): Future[Seq[SafePath]]
  def saveFile(safePath: SafePath, content: String, hash: Option[String] = None, overwrite: Boolean = false)(using BasePath): Future[(Boolean, String)]
  def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean]
  def extractArchive(path: SafePath, to: SafePath)(using BasePath): Future[Unit]
  def listFiles(path: SafePath, filter: FileSorting = FileSorting(), withHidden: Boolean = false)(using BasePath): Future[FileListData]
  def listRecursive(path: SafePath, findString: Option[String], withHidden: Boolean = false)(using BasePath): Future[Seq[(SafePath, Boolean)]]
  def move(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath): Future[Seq[SafePath]]
  def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit]
  def exists(path: SafePath)(using BasePath): Future[Boolean]
  def isTextFile(path: SafePath)(using BasePath): Future[Boolean]
  def temporaryDirectory()(using BasePath): Future[SafePath]

  def executionState(id: Seq[ExecutionId] = Seq())(using BasePath): Future[Seq[ExecutionData]]
  def executionOutput(id: ExecutionId, line: Int)(using BasePath): Future[ExecutionOutput]

  def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit]
  def removeExecution(id: ExecutionId)(using BasePath): Future[Unit]
  def validateScript(script: SafePath)(using BasePath): Future[Option[ErrorData]]
  def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId]
  def clearEnvironmentError(id: ExecutionId, environment: EnvironmentId)(using BasePath): Future[Unit]
  def listEnvironmentError(id: ExecutionId, environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentError]]

  def listPlugins()(using BasePath): Future[Seq[Plugin]]
  def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]]
  def removePlugin(path: SafePath)(using BasePath): Future[Unit]

  def omrMethod(path: SafePath)(using BasePath): Future[Option[String]]
  def omrContent(path: SafePath, data: Option[String] = None)(using BasePath): Future[GUIOMRContent]
  def omrFiles(path: SafePath)(using BasePath): Future[Option[SafePath]]
  def omrDataIndex(path: SafePath)(using BasePath): Future[Seq[GUIOMRDataIndex]]

  def fetchGUIPlugins(f: GUIPlugins => Unit)(using BasePath): Future[Unit]

  def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean)(using BasePath): Future[Unit]

  def marketIndex()(using BasePath): Future[MarketIndex]
  def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit]

  def omSettings()(using BasePath): Future[OMSettings]
  def shutdown()(using BasePath): Future[Unit]
  
  def jvmInfos()(using BasePath): Future[JVMInfos]
  def listNotification()(using BasePath): Future[Seq[NotificationEvent]]
  def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit]
  def removeContainerCache()(using BasePath): Future[Unit]

  def mdToHtml(safePath: SafePath)(using BasePath): Future[String]
  def sequence(safePath: SafePath)(using BasePath): Future[SequenceData]

  def upload(fileList: Seq[(org.scalajs.dom.File, SafePath)], fileTransferState: ProcessState => Unit = _ => ())(using BasePath): Future[Seq[(RelativePath, SafePath)]]
  def download(safePath: SafePath, fileTransferState: ProcessState => Unit = _ => (), hash: Boolean = false)(using BasePath): Future[(String, Option[String])]

  def cloneRepository(repository: String, destination: SafePath, overwrite: Boolean)(using BasePath): Future[Option[SafePath]]
  def commitFiles(files: Seq[SafePath], message: String)(using BasePath): Future[Unit]
  def revertFiles(files: Seq[SafePath])(using BasePath): Future[Unit]
  def addFiles(files: Seq[SafePath])(using BasePath): Future[Unit]
  def pull(from: SafePath)(using BasePath): Future[MergeStatus]
  def push(from: SafePath)(using BasePath): Future[PushStatus]
  def branchList(from: SafePath)(using BasePath): Future[Option[BranchData]]
  def checkout(from: SafePath, branchName: String)(using BasePath): Future[Unit]
  def stash(from: SafePath)(using BasePath): Future[Unit]
  def stashPop(from: SafePath)(using BasePath): Future[MergeStatus]

  def gitAuthentications()(using BasePath): Future[Seq[GitPrivateKeyAuthenticationData]]
  def addGitAuthentication(data: GitPrivateKeyAuthenticationData)(using BasePath): Future[Unit]
  def removeGitAuthentication(data: GitPrivateKeyAuthenticationData, delete: Boolean)(using BasePath): Future[Unit]
  def testGitAuthentication(data: GitPrivateKeyAuthenticationData)(using BasePath): Future[Seq[Test]]
