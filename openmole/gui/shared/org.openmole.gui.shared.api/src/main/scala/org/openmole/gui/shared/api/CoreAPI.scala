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

trait CoreAPI extends RESTAPI {


  // ----- Workspace -------

//  val isPasswordCorrect: Endpoint[String, Boolean] =
//    endpoint(post(path / "is-password-correct", jsonRequest[String]), ok(jsonResponse[Boolean]))

//  val resetPassword: Endpoint[Unit, Unit] =
//    endpoint(get(path / "reset-password"), ok(jsonResponse[Unit]))

  // ------ Files ------------

  val size: Endpoint[SafePath, Long] =
    endpoint(post(path / "file" / "size", jsonRequest[SafePath]), ok(jsonResponse[Long]))

  //def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String)
//  implicit lazy val saveFileRequestSchema: JsonSchema[(SafePath, String, Option[String], Boolean)] = genericJsonSchema
  val saveFile: Endpoint[(SafePath, String, Option[String], Boolean), (Boolean, String)] =
    endpoint(post(path / "file"/ "save", jsonRequest[(SafePath, String, Option[String], Boolean)]), ok(jsonResponse[(Boolean, String)]))

  //def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = {
  val copyFiles: Endpoint[(Seq[SafePath], SafePath, Boolean), Seq[SafePath]] =
    endpoint(post(path / "file" / "copy", jsonRequest[(Seq[SafePath], SafePath, Boolean)]), ok(jsonResponse[Seq[SafePath]]))

  //def addDirectory(safePath: SafePath, directoryName: String): Boolean
//  val createDirectory: Endpoint[(SafePath, String), Boolean] =
//    endpoint(post(path / "file"  / "make-directory", jsonRequest[(SafePath, String)]), ok(jsonResponse[Boolean]))

  //def addFile(safePath: SafePath, fileName: String): Boolean
  val createFile: Endpoint[(SafePath, String, Boolean), Boolean] =
    endpoint(post(path / "file" / "create", jsonRequest[(SafePath, String, Boolean)]), ok(jsonResponse[Boolean]))

  //def extractTGZ(safePath: SafePath): ExtractResult
  val extract: Endpoint[SafePath, Option[ErrorData]] =
    endpoint(post(path / "file" / "extract", jsonRequest[SafePath]), ok(jsonResponse[Option[ErrorData]]))

  //def recursiveListFiles(path: SafePath, findString: String = ""): Seq[(SafePath, Boolean)]
  val listFiles: Endpoint[(SafePath, FileFilter), ListFilesData] =
    endpoint(post(path / "file" / "list", jsonRequest[(SafePath, FileFilter)]), ok(jsonResponse[ListFilesData]))

  val listRecursive: Endpoint[(SafePath, Option[String]), Seq[(SafePath, Boolean)]] =
    endpoint(post(path / "file" / "list-recursive", jsonRequest[(SafePath, Option[String])]), ok(jsonResponse[Seq[(SafePath, Boolean)]]))

  //  def isEmpty(safePath: SafePath): Boolean
  val move: Endpoint[(SafePath, SafePath), Unit] =
    endpoint(post(path / "file" / "move", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Unit]))

  val duplicate: Endpoint[(SafePath, String), SafePath] =
   endpoint(post(path / "file" / "duplicate", jsonRequest[(SafePath, String)]), ok(jsonResponse[SafePath]))

  val deleteFiles: Endpoint[Seq[SafePath], Unit] =
    endpoint(post(path / "file" / "delete", jsonRequest[Seq[SafePath]]), ok(jsonResponse[Unit]))

  val exists: Endpoint[SafePath, Boolean] =
    endpoint(post(path / "file" / "exists", jsonRequest[SafePath]), ok(jsonResponse[Boolean]))

  val temporaryDirectory: Endpoint[Unit, SafePath] =
    endpoint(get(path / "file" / "temporary-directory"), ok(jsonResponse[SafePath]))


  //  val rename: Endpoint[(SafePath, String), SafePath] =
  //    endpoint(post(path / "rename", jsonRequest[(SafePath, String)]), ok(jsonResponse[SafePath]))


  //  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit
//  val copyAllFromTmp: Endpoint[(SafePath, SafePath), Unit] =
//    endpoint(post(path / "copy-from-tmp", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Unit]))

  //  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath]
//  val extractTestExist: Endpoint[(SafePath, SafePath), Seq[SafePath]] =
//    endpoint(post(path / "extract-test-exist", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Seq[SafePath]]))


  //  def deleteFile(safePath: SafePath, context: ServerFileSystemContext): Unit
  //  def deleteFiles(safePath: Seq[SafePath], context: ServerFileSystemContext): Unit
  //implicit lazy val deleteFileRequestSchema: JsonSchema[(Seq[SafePath], ServerFileSystemContext)] = genericJsonSchema

  //def existsExcept(exception: SafePath, exceptItSelf: Boolean): Boolean
//  val existsExcept: Endpoint[(SafePath, Boolean), Boolean] =
//    endpoint(post(path / "exists-except", jsonRequest[(SafePath, Boolean)]), ok(jsonResponse[Boolean]))




  // ---------- Executions --------------------
  //def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])
//  lazy val allStatesResponseSchema: JsonSchema[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = genericJsonSchema
  val allStates: Endpoint[Int, (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] =
    endpoint(post(path / "execution" / "state", jsonRequest[Int]), ok(jsonResponse[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])]))

//  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]
  val staticInfos: Endpoint[Unit, Seq[(ExecutionId, StaticExecutionInfo)]] =
    endpoint(get(path / "execution" / "info"), ok(jsonResponse[Seq[(ExecutionId, StaticExecutionInfo)]]))

//  def cancelExecution(id: ExecutionId): Unit
  val cancelExecution: Endpoint[ExecutionId, Unit] =
    endpoint(post(path / "execution" / "cancel", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def removeExecution(id: ExecutionId): Unit
  val removeExecution: Endpoint[ExecutionId, Unit] =
    endpoint(post(path / "execution" / "remove", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def compileScript(scriptData: ScriptData): Option[ErrorData]
  val compileScript: Endpoint[SafePath, Option[ErrorData]] =
    endpoint(post(path / "execution" / "compile", jsonRequest[SafePath]), ok(jsonResponse[Option[ErrorData]]))

//  def runScript(scriptData: ScriptData, validateScript: Boolean): Unit
  val runScript: Endpoint[(SafePath, Boolean), Unit] =
    endpoint(post(path / "execution" / "run", jsonRequest[(SafePath, Boolean)]), ok(jsonResponse[Unit]))

//  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit
  val clearEnvironmentErrors: Endpoint[EnvironmentId, Unit] =
    endpoint(post(path / "execution" / "clear-environment-error", jsonRequest[EnvironmentId]), ok(jsonResponse[Unit]))

//  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData
  val runningErrorEnvironmentData: Endpoint[(EnvironmentId, Int), EnvironmentErrorData] =
    endpoint(post(path / "execution" / "get-environment-error", jsonRequest[(EnvironmentId, Int)]), ok(jsonResponse[EnvironmentErrorData]))

  // ---- Plugins -----
  val listPlugins: Endpoint[Unit, Seq[Plugin]] =
    endpoint(get(path / "plugin" / "list"), ok(jsonResponse[Seq[Plugin]]))

  val guiPlugins: Endpoint[Unit, PluginExtensionData] =
    endpoint(get(path / "plugin" / "gui"), ok(jsonResponse[PluginExtensionData]))

  val addPlugin: Endpoint[SafePath, Seq[ErrorData]] =
    endpoint(post(path / "plugin" / "add", jsonRequest[SafePath]), ok(jsonResponse[Seq[ErrorData]]))

  val removePlugin: Endpoint[SafePath, Unit] =
    endpoint(post(path / "plugin" / "remove", jsonRequest[SafePath]), ok(jsonResponse[Unit]))

  val omrMethod: Endpoint[SafePath, String] =
    endpoint(post(path / "plugin" / "omr-method", jsonRequest[SafePath]), ok(jsonResponse[String]))

  // ---- Model Wizards --------------
  //def models(archivePath: SafePath): Seq[SafePath]
  val models: Endpoint[SafePath, Seq[SafePath]] =
    endpoint(post(path / "wizard" / "models", jsonRequest[SafePath]), ok(jsonResponse[Seq[SafePath]]))

  //def expandResources(resources: Resources): Resources
  val expandResources: Endpoint[Resources, Resources] =
    endpoint(post(path / "wizard" / "expand-resources", jsonRequest[Resources]), ok(jsonResponse[Resources]))

  //def downloadHTTP(url: String, path: SafePath, extract: Boolean): Either[Unit, ErrorData]
  val downloadHTTP: Endpoint[(String, SafePath, Boolean), Option[ErrorData]] =
    endpoint(post(path / "wizard" / "download-http", jsonRequest[(String, SafePath, Boolean)]), ok(jsonResponse[Option[ErrorData]]))

  // ---------- Market ----------

  //def marketIndex(): MarketIndex
  val marketIndex: Endpoint[Unit, MarketIndex] =
    endpoint(get(path / "market" / "index"), ok(jsonResponse[MarketIndex]))

//    def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit
  val getMarketEntry: Endpoint[(MarketIndexEntry, SafePath), Unit] =
    endpoint(post(path / "market" / "get-entry", jsonRequest[(MarketIndexEntry, SafePath)]), ok(jsonResponse[Unit]))


  // ---------- Application ------------

  val omSettings: Endpoint[Unit, OMSettings] =
    endpoint(get(path / "application" / "settings"), ok(jsonResponse[OMSettings]))

  // def shutdown(): Unit
  val shutdown: Endpoint[Unit, Unit] =
    endpoint(get(path / "application" / "shutdown"), ok(jsonResponse[Unit]))

  //  def restart(): Unit
  val restart: Endpoint[Unit, Unit] =
    endpoint(get(path / "application" / "restart"), ok(jsonResponse[Unit]))

  //  def isAlive(): Boolean
  val isAlive: Endpoint[Unit, Boolean] =
    endpoint(get(path / "application" / "is-alive"), ok(jsonResponse[Boolean]))

  //  def jvmInfos(): JVMInfos
  val jvmInfos: Endpoint[Unit, JVMInfos] =
    endpoint(get(path / "application" / "jvm-infos"), ok(jsonResponse[JVMInfos]))


  //def mdToHtml(safePath: SafePath): String
  val mdToHtml: Endpoint[SafePath, String] =
    endpoint(post(path / "tool" / "md-to-html", jsonRequest[SafePath]), ok(jsonResponse[String]))

  //def copyFromTmp(tmpSafePath: SafePath, filesToBeMoved: Seq[SafePath]): Unit

  //def renameFile(safePath: SafePath, name: String): SafePath

  //def sequence(safePath: SafePath, separator: Char = ','): SequenceData
  val sequence: Endpoint[SafePath, SequenceData] =
    endpoint(post(path / "tool" / "sequence", jsonRequest[SafePath]), ok(jsonResponse[SequenceData]))


  //TODO ------------ refactor -------------------
  // def appendToPluggedIfPlugin(safePath: SafePath): Unit =

}
