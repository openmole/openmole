package org.openmole.gui.ext.api

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

import endpoints4s.{algebra, generic}
import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.ext.data.*

object NewAPI {
  case class Test(uuid: String)
}

trait NewAPI extends algebra.Endpoints
  with algebra.JsonEntitiesFromSchemas
  with generic.JsonSchemas
  with DataJsonSchemas {

  val uuid: Endpoint[Unit, NewAPI.Test] =
    endpoint(get(path / "uuid"), ok(jsonResponse[NewAPI.Test]))

  val omSettings: Endpoint[Unit, OMSettings] =
    endpoint(get(path / "settings"), ok(jsonResponse[OMSettings]))

  // ----- Workspace -------

  val isPasswordCorrect: Endpoint[String, Boolean] =
    endpoint(post(path / "is-password-correct", jsonRequest[String]), ok(jsonResponse[Boolean]))

  val resetPassword: Endpoint[Unit, Unit] =
    endpoint(get(path / "reset-password"), ok(jsonResponse[Unit]))

  // ------ Files ------------

  val listFiles: Endpoint[(SafePath, FileFilter), ListFilesData] =
    endpoint(post(path / "list-files", jsonRequest[(SafePath,FileFilter)]), ok(jsonResponse[ListFilesData]))

  val size: Endpoint[SafePath, Long] =
    endpoint(post(path / "size", jsonRequest[SafePath]), ok(jsonResponse[Long]))

  //def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String)
  implicit lazy val saveFileRequestSchema: JsonSchema[(SafePath, String, Option[String], Boolean)] = genericJsonSchema
  val saveFile: Endpoint[(SafePath, String, Option[String], Boolean), (Boolean, String)] =
    endpoint(post(path / "save-file", jsonRequest[(SafePath, String, Option[String], Boolean)]), ok(jsonResponse[(Boolean, String)]))

  //def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = {
  val copyProjectFiles: Endpoint[(Seq[SafePath], SafePath, Boolean), Seq[SafePath]] =
    endpoint(post(path / "copy-project-file", jsonRequest[(Seq[SafePath], SafePath, Boolean)]), ok(jsonResponse[Seq[SafePath]]))

  //def addDirectory(safePath: SafePath, directoryName: String): Boolean
  val createDirectory: Endpoint[(SafePath, String), Boolean] =
    endpoint(post(path / "create-directory", jsonRequest[(SafePath, String)]), ok(jsonResponse[Boolean]))

  //def addFile(safePath: SafePath, fileName: String): Boolean
  val createFile: Endpoint[(SafePath, String), Boolean] =
    endpoint(post(path / "create-file", jsonRequest[(SafePath, String)]), ok(jsonResponse[Boolean]))

  //def extractTGZ(safePath: SafePath): ExtractResult
  val extract: Endpoint[SafePath, Option[ErrorData]] =
    endpoint(post(path / "extract", jsonRequest[SafePath]), ok(jsonResponse[Option[ErrorData]]))

  //def recursiveListFiles(path: SafePath, findString: String = ""): Seq[(SafePath, Boolean)]

  implicit lazy val listRecursiveRequestSchema: JsonSchema[(SafePath, Option[String])] = genericJsonSchema
//  implicit lazy val listRecursiveResponseSchema: JsonSchema[(SafePath, Boolean)] = genericJsonSchema
  val listRecursive: Endpoint[(SafePath, Option[String]), Seq[(SafePath, Boolean)]] =
    endpoint(post(path / "list-recursive", jsonRequest[(SafePath, Option[String])]), ok(jsonResponse[Seq[(SafePath, Boolean)]]))

  //  def isEmpty(safePath: SafePath): Boolean
  //  def move(from: SafePath, to: SafePath): Unit
  val move: Endpoint[(SafePath, SafePath), Unit] =
    endpoint(post(path / "move", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Unit]))

  //  def duplicate(safePath: SafePath, newName: String): SafePath
  val duplicate: Endpoint[(SafePath, String), SafePath] =
   endpoint(post(path / "duplicate", jsonRequest[(SafePath, String)]), ok(jsonResponse[SafePath]))

  //  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit


  //  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath]
//  val extractTestExist: Endpoint[(SafePath, SafePath), Seq[SafePath]] =
//    endpoint(post(path / "extract-test-exist", jsonRequest[(SafePath, SafePath)]), ok(jsonResponse[Seq[SafePath]]))


  //  def deleteFile(safePath: SafePath, context: ServerFileSystemContext): Unit
  //  def deleteFiles(safePath: Seq[SafePath], context: ServerFileSystemContext): Unit
  //implicit lazy val deleteFileRequestSchema: JsonSchema[(Seq[SafePath], ServerFileSystemContext)] = genericJsonSchema
  val deleteFiles: Endpoint[(Seq[SafePath], ServerFileSystemContext), Unit] =
    endpoint(post(path / "delete-files", jsonRequest[(Seq[SafePath], ServerFileSystemContext)]), ok(jsonResponse[Unit]))


  //def exists(safePath: SafePath): Boolean
  val exists: Endpoint[SafePath, Boolean] =
    endpoint(post(path / "exists", jsonRequest[SafePath]), ok(jsonResponse[Boolean]))

  //def existsExcept(exception: SafePath, exceptItSelf: Boolean): Boolean
  val existsExcept: Endpoint[(SafePath, Boolean), Boolean] =
    endpoint(post(path / "exists-except", jsonRequest[(SafePath, Boolean)]), ok(jsonResponse[Boolean]))


  //def mdToHtml(safePath: SafePath): String
  val mdToHtml: Endpoint[SafePath, String] =
    endpoint(post(path / "md-to-html", jsonRequest[SafePath]), ok(jsonResponse[String]))

  //def copyFromTmp(tmpSafePath: SafePath, filesToBeMoved: Seq[SafePath]): Unit

  //def renameFile(safePath: SafePath, name: String): SafePath
  val rename: Endpoint[(SafePath, String), SafePath] =
    endpoint(post(path / "rename", jsonRequest[(SafePath, String)]), ok(jsonResponse[SafePath]))

  //def sequence(safePath: SafePath, separator: Char = ','): SequenceData
  val sequence: Endpoint[SafePath, SequenceData] =
    endpoint(post(path / "sequence", jsonRequest[SafePath]), ok(jsonResponse[SequenceData]))


  // ---------- Executions --------------------
  //def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])
//  lazy val allStatesResponseSchema: JsonSchema[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = genericJsonSchema
  val allStates: Endpoint[Int, (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] =
    endpoint(post(path / "all-states", jsonRequest[Int]), ok(jsonResponse[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])]))

//  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]
  val staticInfos: Endpoint[Unit, Seq[(ExecutionId, StaticExecutionInfo)]] =
    endpoint(get(path / "static-execution-info"), ok(jsonResponse[Seq[(ExecutionId, StaticExecutionInfo)]]))

//  def cancelExecution(id: ExecutionId): Unit
  val cancelExecution: Endpoint[ExecutionId, Unit] =
    endpoint(post(path / "cancel-execution", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def removeExecution(id: ExecutionId): Unit
  val removeExecution: Endpoint[ExecutionId, Unit] =
    endpoint(post(path / "remove-execution", jsonRequest[ExecutionId]), ok(jsonResponse[Unit]))

//  def compileScript(scriptData: ScriptData): Option[ErrorData]
  val compileScript: Endpoint[ScriptData, Option[ErrorData]] =
    endpoint(post(path / "compile-script", jsonRequest[ScriptData]), ok(jsonResponse[Option[ErrorData]]))

//  def runScript(scriptData: ScriptData, validateScript: Boolean): Unit
  val runScript: Endpoint[(ScriptData, Boolean), Unit] =
    endpoint(post(path / "run-script", jsonRequest[(ScriptData, Boolean)]), ok(jsonResponse[Unit]))

//  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit
  val clearEnvironmentErrors: Endpoint[EnvironmentId, Unit] =
    endpoint(post(path / "clear-environment-errors", jsonRequest[EnvironmentId]), ok(jsonResponse[Unit]))

//  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData
  val runningErrorEnvironmentData: Endpoint[(EnvironmentId, Int), EnvironmentErrorData] =
    endpoint(post(path / "running-environment-error", jsonRequest[(EnvironmentId, Int)]), ok(jsonResponse[EnvironmentErrorData]))


  // ---- Authentication ---------

  //def renameKey(keyName: String, newName: String): Unit
  val renameKey =
    endpoint(post(path / "rename-key", jsonRequest[(String, String)]), ok(jsonResponse[Unit]))

  // ---- Plugins -----
  val listPlugins: Endpoint[Unit, Iterable[Plugin]] =
    endpoint(get(path / "list-plugins"), ok(jsonResponse[Iterable[Plugin]]))

  val guiPlugins: Endpoint[Unit, PluginExtensionData] =
    endpoint(get(path / "gui-plugins"), ok(jsonResponse[PluginExtensionData]))


  // To port
  //def appendToPluggedIfPlugin(safePath: SafePath): Unit
  //def unplug(safePath: SafePath): Unit
  //def isOSGI(safePath: SafePath): Boolean
  //def findAnalysisPlugin(result: SafePath): Option[GUIPluginAsJS]


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

  // FIXME wait for scala 3

//  //def marketIndex(): MarketIndex
//  val marketIndex: Endpoint[Unit, MarketIndex] =
//    endpoint(get(path / "market" / "index"), ok(jsonResponse[MarketIndex]))
//
////    def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit
//  val getMarketEntry: Endpoint[(MarketIndexEntry, SafePath), Unit] =
//    endpoint(post(path / "market" / "get-entry", jsonRequest[(MarketIndexEntry, SafePath)]), ok(jsonResponse[Unit]))


  // ---------- Application ------------

  //  def shutdown(): Unit
  //  def restart(): Unit
  //  def isAlive(): Boolean
  //  def jvmInfos(): JVMInfos

  //TODO ------------ refactor -------------------
  // def appendToPluggedIfPlugin(safePath: SafePath): Unit =

  implicit lazy val fooSchema: JsonSchema[NewAPI.Test] = genericJsonSchema
  implicit lazy val omSettingsSchema: JsonSchema[OMSettings] = genericJsonSchema
  implicit lazy val pluginSchema: JsonSchema[Plugin] = genericJsonSchema
  implicit lazy val allPluginExtensionDataSchema: JsonSchema[PluginExtensionData] = genericJsonSchema
  implicit lazy val fileFilterSchema: JsonSchema[FileFilter] = genericJsonSchema
  implicit lazy val listFilesDataSchema: JsonSchema[ListFilesData] = genericJsonSchema
  implicit lazy val firstLastSchema: JsonSchema[FirstLast] = genericJsonSchema
  implicit lazy val listSorting: JsonSchema[ListSorting] = genericJsonSchema
  implicit lazy val treeNodeDataSchema: JsonSchema[TreeNodeData] = genericJsonSchema
  implicit lazy val dirDataSchema: JsonSchema[DirData] = genericJsonSchema
  implicit lazy val pluginStateSchema: JsonSchema[PluginState] = genericJsonSchema
  implicit lazy val sequenceDataSchema: JsonSchema[SequenceData] = genericJsonSchema
  implicit lazy val executionIdSchema: JsonSchema[ExecutionId] = genericJsonSchema
  implicit lazy val jobStatusesSchema: JsonSchema[ExecutionInfo.JobStatuses] = genericJsonSchema
  implicit lazy val capsuleExecutionSchema: JsonSchema[ExecutionInfo.CapsuleExecution] = genericJsonSchema
  implicit lazy val environmentIdSchema: JsonSchema[EnvironmentId] = genericJsonSchema
  implicit lazy val networkActivitySchema: JsonSchema[NetworkActivity] = genericJsonSchema
  implicit lazy val executionActivitySchema: JsonSchema[ExecutionActivity] = genericJsonSchema
  implicit lazy val environmentStateSchema: JsonSchema[EnvironmentState] = genericJsonSchema
  implicit lazy val executionInfoSchema: JsonSchema[ExecutionInfo] = genericJsonSchema
  implicit lazy val outputStreamDataSchema: JsonSchema[OutputStreamData] = genericJsonSchema
  implicit lazy val staticExecutionInfoSchema: JsonSchema[StaticExecutionInfo] = genericJsonSchema
  implicit lazy val scriptDataSchema: JsonSchema[ScriptData] = genericJsonSchema
  implicit lazy val errorStateLevelSchema: JsonSchema[ErrorStateLevel] = genericJsonSchema
  implicit lazy val environmentErrorSchema: JsonSchema[EnvironmentError] = genericJsonSchema
  implicit lazy val environmentErrorDataSchema: JsonSchema[EnvironmentErrorData] = genericJsonSchema
  implicit lazy val resourceSchema: JsonSchema[Resource] = genericJsonSchema
  implicit lazy val resourcesSchema: JsonSchema[Resources] = genericJsonSchema

  // Uncomment when scala 3
//  implicit lazy val marketIndexEntrySchema: JsonSchema[MarketIndexEntry] = genericJsonSchema
//  implicit lazy val marketIndexSchema: JsonSchema[MarketIndex] = genericJsonSchema

}
