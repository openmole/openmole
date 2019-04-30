/*
 * Copyright (C) 30/07/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.gui.ext.api

import org.openmole.core.buildinfo._
import org.openmole.core.market.{ MarketIndex, MarketIndexEntry }
import org.openmole.gui.ext.data._

trait Api {

  //  //GENERAL
  def settings(): OMSettings
  def shutdown(): Unit
  def restart(): Unit
  def isAlive(): Boolean
  def jvmInfos(): JVMInfos

  //AUTHENTICATIONS
  def renameKey(keyName: String, newName: String): Unit

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean
  def resetPassword(): Unit
  //  def getConfigurationValue(configData: ConfigData): Option[String]
  //  def setConfigurationValue(configData: ConfigData, value: String): Unit

  //FILES
  def addDirectory(safePath: SafePath, directoryName: String): Boolean
  def addFile(safePath: SafePath, fileName: String): Boolean
  def extractTGZ(safePath: SafePath): ExtractResult
  def deleteFile(safePath: SafePath, context: ServerFileSystemContext): Unit
  def deleteFiles(safePath: Seq[SafePath], context: ServerFileSystemContext): Unit
  def temporaryFile(): SafePath
  def exists(safePath: SafePath): Boolean
  def existsExcept(exception: SafePath, exceptItSelf: Boolean): Boolean
  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath]
  def safePath(safePaths: Seq[SafePath]): Seq[SafePath]
  def listFiles(path: SafePath, fileFilter: FileFilter = FileFilter()): ListFilesData
  def isEmpty(safePath: SafePath): Boolean
  def mdToHtml(safePath: SafePath): String
  def move(from: SafePath, to: SafePath): Unit
  def duplicate(safePath: SafePath, newName: String): SafePath
  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit
  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath]
  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Unit
  def copyToPluginUploadDir(safePaths: Seq[SafePath]): Unit
  def copyFromTmp(tmpSafePath: SafePath, filesToBeMoved: Seq[SafePath]): Unit
  def uuid(): String = java.util.UUID.randomUUID.toString
  def renameFile(safePath: SafePath, name: String): SafePath
  def saveFile(path: SafePath, fileContent: String): Unit
  def saveFiles(fileContents: Seq[AlterableFileContent]): Unit
  def size(safePath: SafePath): Long
  def sequence(safePath: SafePath, separator: Char = ','): SequenceData

  //EXECUTIONS
  def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])
  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]
  def cancelExecution(id: ExecutionId): Unit
  def removeExecution(id: ExecutionId): Unit
  def compileScript(scriptData: ScriptData): Option[ErrorData]
  def runScript(scriptData: ScriptData, validateScript: Boolean): Unit
  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit
  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData

  //MARKET
  def marketIndex(): MarketIndex
  def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit

  //CORE PLUGINS
  def addUploadedPlugins(nodes: Seq[String]): Seq[ErrorData]
  def autoAddPlugins(path: SafePath): Unit
  def isPlugin(path: SafePath): Boolean
  def allPluggableIn(path: SafePath): Seq[SafePath]
  def listPlugins(): Iterable[Plugin]
  def removePlugin(plugin: Plugin): Unit

  //GUI PLUGINS
  def getGUIPlugins(): AllPluginExtensionData
  def isOSGI(safePath: SafePath): Boolean

  //MODEL WIZARDS
  def models(archivePath: SafePath): Seq[SafePath]
  def expandResources(resources: Resources): Resources
}
