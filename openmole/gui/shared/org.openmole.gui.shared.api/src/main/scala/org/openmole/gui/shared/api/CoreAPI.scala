package org.openmole.gui.shared.api

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

trait CoreAPI extends RESTAPI:

  def prefix = "gui"
  
  // ----- Workspace -------

//  val isPasswordCorrect: Endpoint[String, Boolean] =
//    endpoint(post(path / "is-password-correct", jsonRequest[String]), ok(jsonResponse[Boolean]))

//  val resetPassword: Endpoint[Unit, Unit] =
//    endpoint(get(path / "reset-password"), ok(jsonResponse[Unit]))

  // ------ Files ------------

  val size: ErrorEndpoint[SafePath, Long] =
    errorEndpoint(post(path / prefix / "file" / "size", jsonRequest[SafePath]), ok(jsonResponse[Long]))

  //def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String)
//  implicit lazy val saveFileRequestSchema: JsonSchema[(SafePath, String, Option[String], Boolean)] = genericJsonSchema
  val saveFile: ErrorEndpoint[(SafePath, String, Option[String], Boolean), (Boolean, String)] =
    errorEndpoint(post(path / prefix / "file"/ "save", jsonRequest[(SafePath, String, Option[String], Boolean)]), ok(jsonResponse[(Boolean, String)]))

  //def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = {
  val copyFiles: ErrorEndpoint[(Seq[(SafePath, SafePath)], Boolean), Seq[SafePath]] =
    errorEndpoint(post(path / prefix / "file" / "copy", jsonRequest[(Seq[(SafePath, SafePath)], Boolean)]), ok(jsonResponse[Seq[SafePath]]))

  //def addFile(safePath: SafePath, fileName: String): Boolean
  val createFile: ErrorEndpoint[(SafePath, String, Boolean), Boolean] =
    errorEndpoint(post(path / prefix / "file" / "create", jsonRequest[(SafePath, String, Boolean)]), ok(jsonResponse[Boolean]))

  //def extractTGZ(safePath: SafePath): ExtractResult
  val extractArchive: ErrorEndpoint[(SafePath, SafePath), Unit] =
    errorEndpoint(post(path / prefix / "file" / "extract-archive", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Unit]))

  //def recursiveListFiles(path: SafePath, findString: String = ""): Seq[(SafePath, Boolean)]
  val listFiles: ErrorEndpoint[(SafePath, FileSorting, Boolean), FileListData] =
    errorEndpoint(post(path / prefix / "file" / "list", jsonRequest[(SafePath, FileSorting, Boolean)]), ok(jsonResponse[FileListData]))

  val listRecursive: ErrorEndpoint[(SafePath, Option[String], Boolean), Seq[(SafePath, Boolean)]] =
    errorEndpoint(post(path / prefix / "file" / "list-recursive", jsonRequest[(SafePath, Option[String], Boolean)]), ok(jsonResponse[Seq[(SafePath, Boolean)]]))

  //  def isEmpty(safePath: SafePath): Boolean
  val move: ErrorEndpoint[(Seq[(SafePath, SafePath)], Boolean), Seq[SafePath]] =
    errorEndpoint(post(path / prefix / "file" / "move", jsonRequest[(Seq[(SafePath, SafePath)], Boolean)]), ok(jsonResponse[Seq[SafePath]]))

  val duplicate: ErrorEndpoint[(SafePath, String), SafePath] =
   errorEndpoint(post(path / prefix / "file" / "duplicate", jsonRequest[(SafePath, String)]), ok(jsonResponse[SafePath]))

  val deleteFiles: ErrorEndpoint[Seq[SafePath], Unit] =
    errorEndpoint(post(path / prefix / "file" / "delete", jsonRequest[Seq[SafePath]]), ok(jsonResponse[Unit]))

  val exists: ErrorEndpoint[SafePath, Boolean] =
    errorEndpoint(post(path / prefix / "file" / "exists", jsonRequest[SafePath]), ok(jsonResponse[Boolean]))

  val isText: ErrorEndpoint[SafePath, Boolean] =
    errorEndpoint(post(path / prefix / "file" / "is-text", jsonRequest[SafePath]), ok(jsonResponse[Boolean]))

  val temporaryDirectory: ErrorEndpoint[Unit, SafePath] =
    errorEndpoint(get(path / prefix / "file" / "temporary-directory"), ok(jsonResponse[SafePath]))

  val omrMethod: ErrorEndpoint[SafePath, Option[String]] =
    errorEndpoint(post(path / prefix / "file" / "omr" / "method", jsonRequest[SafePath]), ok(jsonResponse[Option[String]]))

  val omrContent: ErrorEndpoint[(SafePath, Option[String]), GUIOMRContent] =
    errorEndpoint(post(path / prefix / "file" / "omr" / "content", jsonRequest[(SafePath, Option[String])]), ok(jsonResponse[GUIOMRContent]))

  val omrFiles: ErrorEndpoint[SafePath, Option[SafePath]] =
    errorEndpoint(post(path / prefix / "file" / "omr" / "files", jsonRequest[SafePath]), ok(jsonResponse[Option[SafePath]]))

  val omrDataIndex: ErrorEndpoint[SafePath, Seq[GUIOMRDataIndex]] =
    errorEndpoint(post(path / prefix / "file" / "omr" / "index", jsonRequest[SafePath]), ok(jsonResponse[Seq[GUIOMRDataIndex]]))

  val cloneRepository: ErrorEndpoint[(String, SafePath), Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "clone", jsonRequest[(String,SafePath)]), ok(jsonResponse[Unit]))

  val commit: ErrorEndpoint[(Seq[SafePath], String), Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "commit", jsonRequest[(Seq[SafePath], String)]), ok(jsonResponse[Unit]))
    
  val revert: ErrorEndpoint[Seq[SafePath], Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "revert", jsonRequest[Seq[SafePath]]), ok(jsonResponse[Unit]))

  val add: ErrorEndpoint[Seq[SafePath], Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "add", jsonRequest[Seq[SafePath]]), ok(jsonResponse[Unit]))

  val pull: ErrorEndpoint[SafePath, MergeStatus] =
    errorEndpoint(post(path / prefix / "file" / "git" / "pull", jsonRequest[SafePath]), ok(jsonResponse[MergeStatus]))
  
  val push: ErrorEndpoint[SafePath, PushStatus] =
    errorEndpoint(post(path / "git" / "push", jsonRequest[SafePath]), ok(jsonResponse[PushStatus]))

  val branchList: ErrorEndpoint[SafePath, Option[BranchData]] =
    errorEndpoint(post(path / prefix / "file" / "git" / "branch-list", jsonRequest[SafePath]), ok(jsonResponse[Option[BranchData]]))
    
  val checkout: ErrorEndpoint[(SafePath, String), Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "checkout", jsonRequest[(SafePath, String)]), ok(jsonResponse[Unit]))
    
  val stash: ErrorEndpoint[SafePath, Unit] =
    errorEndpoint(post(path / prefix / "file" / "git" / "stash", jsonRequest[SafePath]), ok(jsonResponse[Unit]))

  val stashPop: ErrorEndpoint[SafePath, MergeStatus] =
    errorEndpoint(post(path / prefix / "file" / "git" / "stash-pop", jsonRequest[SafePath]), ok(jsonResponse[MergeStatus]))

  val gitAuthentications: ErrorEndpoint[Unit, Seq[GitPrivateKeyAuthenticationData]] =
    errorEndpoint(get(path /  "git" / "authentications"), ok(jsonResponse[Seq[GitPrivateKeyAuthenticationData]]))

  val addGitAuthentication: ErrorEndpoint[GitPrivateKeyAuthenticationData, Unit] =
    errorEndpoint(post(path / "git" / "add-authentication", jsonRequest[GitPrivateKeyAuthenticationData]), ok(jsonResponse[Unit]))

  val removeGitAuthentication: ErrorEndpoint[(GitPrivateKeyAuthenticationData, Boolean), Unit] =
    errorEndpoint(post(path / "git" / "remove-authentication", jsonRequest[(GitPrivateKeyAuthenticationData, Boolean)]), ok(jsonResponse[Unit]))

  val testGitAuthentication: ErrorEndpoint[GitPrivateKeyAuthenticationData, Seq[Test]] =
    errorEndpoint(post(path / "git" / "test-authentication", jsonRequest[GitPrivateKeyAuthenticationData]), ok(jsonResponse[Seq[Test]]))


  // ---------- Executions --------------------
  //def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])
//  lazy val allStatesResponseSchema: JsonSchema[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = genericJsonSchema
  val executionState: ErrorEndpoint[Seq[ExecutionId], Seq[ExecutionData]] =
    errorEndpoint(post(path / prefix / "execution" / "state", jsonRequest[Seq[ExecutionId]]), ok(jsonResponse[Seq[ExecutionData]]))

  val executionOutput: ErrorEndpoint[(ExecutionId, Int), ExecutionOutput] =
    errorEndpoint(post(path / prefix / "execution" / "output", jsonRequest[(ExecutionId, Int)]), ok(jsonResponse[ExecutionOutput]))

  //  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]
//  val staticInfos: SafeEndpoint[Unit, Seq[(ExecutionId, StaticExecutionInfo)]] =
//    safeEndpoint(get(path / "execution" / "info"), ok(jsonResponse[Seq[(ExecutionId, StaticExecutionInfo)]]))

//  def cancelExecution(id: ExecutionId): Unit
  val cancelExecution: ErrorEndpoint[ExecutionId, Unit] =
    errorEndpoint(post(path / prefix / "execution" / "cancel", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def removeExecution(id: ExecutionId): Unit
  val removeExecution: ErrorEndpoint[ExecutionId, Unit] =
    errorEndpoint(post(path / prefix / "execution" / "remove", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def compileScript(scriptData: ScriptData): Option[ErrorData]
  val validateScript: ErrorEndpoint[SafePath, Option[ErrorData]] =
    errorEndpoint(post(path / prefix / "execution" / "compile", jsonRequest[SafePath]), ok(jsonResponse[Option[ErrorData]]))

//  def runScript(scriptData: ScriptData, validateScript: Boolean): Unit
  val launchScript: ErrorEndpoint[(SafePath, Boolean), ExecutionId] =
    errorEndpoint(post(path / prefix / "execution" / "launch", jsonRequest[(SafePath, Boolean)]), ok(jsonResponse[ExecutionId]))

//  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit
  val clearEnvironmentErrors: ErrorEndpoint[(ExecutionId, EnvironmentId), Unit] =
    errorEndpoint(post(path / prefix / "execution" / "clear-environment-error", jsonRequest[(ExecutionId, EnvironmentId)]), ok(jsonResponse[Unit]))

//  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData
  val listEnvironmentErrors: ErrorEndpoint[(ExecutionId, EnvironmentId, Int), Seq[EnvironmentError]] =
    errorEndpoint(post(path / prefix / "execution" / "list-environment-error", jsonRequest[(ExecutionId, EnvironmentId, Int)]), ok(jsonResponse[Seq[EnvironmentError]]))

  // ---- Plugins -----
  val listPlugins: ErrorEndpoint[Unit, Seq[Plugin]] =
    errorEndpoint(get(path / prefix / "plugin" / "list"), ok(jsonResponse[Seq[Plugin]]))

  val guiPlugins: ErrorEndpoint[Unit, PluginExtensionData] =
    errorEndpoint(get(path / prefix / "plugin" / "gui"), ok(jsonResponse[PluginExtensionData]))

  val addPlugin: ErrorEndpoint[SafePath, Seq[ErrorData]] =
    errorEndpoint(post(path / prefix / "plugin" / "add", jsonRequest[SafePath]), ok(jsonResponse[Seq[ErrorData]]))

  val removePlugin: ErrorEndpoint[SafePath, Unit] =
    errorEndpoint(post(path / prefix / "plugin" / "remove", jsonRequest[SafePath]), ok(jsonResponse[Unit]))


  // ---- Model Wizards --------------
  //def models(archivePath: SafePath): Seq[SafePath]
//  val models: ErrorEndpoint[SafePath, Seq[SafePath]] =
//    errorEndpoint(post(path / "wizard" / "models", jsonRequest[SafePath]), ok(jsonResponse[Seq[SafePath]]))

  //def expandResources(resources: Resources): Resources
//  val expandResources: ErrorEndpoint[Resources, Resources] =
//    errorEndpoint(post(path / "wizard" / "expand-resources", jsonRequest[Resources]), ok(jsonResponse[Resources]))

  //def downloadHTTP(url: String, path: SafePath, extract: Boolean): Either[Unit, ErrorData]
  val downloadHTTP: ErrorEndpoint[(String, SafePath, Boolean, Boolean), Unit] =
    errorEndpoint(post(path / prefix / "wizard" / "download-http", jsonRequest[(String, SafePath, Boolean, Boolean)]), ok(jsonResponse[Unit]))

  // ---------- Market ----------

  //def marketIndex(): MarketIndex
  val marketIndex: ErrorEndpoint[Unit, MarketIndex] =
    errorEndpoint(get(path / prefix / "market" / "index"), ok(jsonResponse[MarketIndex]))

//    def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit
  val getMarketEntry: ErrorEndpoint[(MarketIndexEntry, SafePath), Unit] =
    errorEndpoint(post(path / prefix / "market" / "get-entry", jsonRequest[(MarketIndexEntry, SafePath)]), ok(jsonResponse[Unit]))


  // ---------- Application ------------

  val omSettings: ErrorEndpoint[Unit, OMSettings] =
    errorEndpoint(get(path / prefix / "application" / "settings"), ok(jsonResponse[OMSettings]))

  // def shutdown(): Unit
  val shutdown: ErrorEndpoint[Unit, Unit] =
    errorEndpoint(get(path / prefix / "application" / "shutdown"), ok(jsonResponse[Unit]))

  //  def restart(): Unit
  val restart: ErrorEndpoint[Unit, Unit] =
    errorEndpoint(get(path / prefix / "application" / "restart"), ok(jsonResponse[Unit]))

  //  def isAlive(): Boolean
  val isAlive: Endpoint[Unit, Boolean] =
    endpoint(get(path / prefix / "application" / "is-alive"), ok(jsonResponse[Boolean]))

  //  def jvmInfos(): JVMInfos
  val jvmInfos: ErrorEndpoint[Unit, JVMInfos] =
    errorEndpoint(get(path / prefix / "application" / "jvm-infos"), ok(jsonResponse[JVMInfos]))

  val listNotification: ErrorEndpoint[Unit, Seq[NotificationEvent]] =
    errorEndpoint(post(path / prefix / "application" / "list-notification", jsonRequest[Unit]), ok(jsonResponse[Seq[NotificationEvent]]))

  val clearNotification: ErrorEndpoint[Seq[Long], Unit] =
    errorEndpoint(post(path / prefix / "application" / "clear-notification", jsonRequest[Seq[Long]]), ok(jsonResponse[Unit]))

  val removeContainerCache: ErrorEndpoint[Unit, Unit] =
    errorEndpoint(post(path / prefix / "application" / "remove-container-cache", jsonRequest[Unit]), ok(jsonResponse[Unit]))


  //def mdToHtml(safePath: SafePath): String
  val mdToHtml: ErrorEndpoint[SafePath, String] =
    errorEndpoint(post(path / prefix / "tool" / "md-to-html", jsonRequest[SafePath]), ok(jsonResponse[String]))

  //def copyFromTmp(tmpSafePath: SafePath, filesToBeMoved: Seq[SafePath]): Unit

  //def renameFile(safePath: SafePath, name: String): SafePath

  //def sequence(safePath: SafePath, separator: Char = ','): SequenceData
  val sequence: ErrorEndpoint[SafePath, SequenceData] =
    errorEndpoint(post(path / prefix / "tool" / "sequence", jsonRequest[SafePath]), ok(jsonResponse[SequenceData]))


//TODO ------------ refactor -------------------
  // def appendToPluggedIfPlugin(safePath: SafePath): Unit =

