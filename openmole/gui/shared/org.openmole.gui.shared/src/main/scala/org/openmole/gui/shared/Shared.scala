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
package org.openmole.gui.shared

import org.openmole.core.buildinfo._
import org.openmole.gui.ext.data._

trait Api {

  //GENERAL
  def settings(): OMSettings

  //AUTHENTICATIONS
  def authentications(): Seq[AuthenticationData]
  def addAuthentication(data: AuthenticationData): Unit
  def removeAuthentication(data: AuthenticationData): Unit
  def testAuthentication(data: AuthenticationData, vos: Seq[String]): Seq[AuthenticationTest]

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean
  def passwordState(): PasswordState
  def resetPassword(): Unit
  def setPassword(pass: String): Boolean
  def getConfigurationValue(configData: ConfigData): Option[String]
  def setConfigurationValue(configData: ConfigData, value: String): Unit

  //FILES
  def addDirectory(treeNode: TreeNodeData, directoryName: String): Boolean
  def addFile(treeNode: TreeNodeData, fileName: String): Boolean
  def extractTGZ(treeNodeData: TreeNodeData): ExtractResult
  def deleteAuthenticationKey(keyName: String): Unit
  def deleteFile(safePath: SafePath, context: ServerFileSytemContext): Unit
  def deleteFiles(safePath: Seq[SafePath], context: ServerFileSytemContext): Unit
  def temporaryFile(): SafePath
  def exists(safePath: SafePath): Boolean
  def existsExcept(exception: TreeNodeData, exceptItSelf: Boolean): Boolean
  def extractAndTestExistence(safePathToTest: SafePath, in: SafePath): Seq[SafePath]
  def treeNodeData(safePaths: Seq[TreeNodeData]): Seq[TreeNodeData]
  def listFiles(path: SafePath, fileFilter: FileFilter = FileFilter()): Seq[TreeNodeData]
  def mdToHtml(safePath: SafePath): String
  def move(from: SafePath, to: SafePath): Unit
  def replicate(treeNodeData: TreeNodeData): TreeNodeData
  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit
  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath]
  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Unit
  def copyToPluginUploadDir(safePaths: Seq[SafePath]): Unit
  def copyFromTmp(tmpSafePath: SafePath, filesToBeMoved: Seq[SafePath]): Unit
  def uuid(): String = java.util.UUID.randomUUID.toString
  def renameFile(treeNode: TreeNodeData, name: String): TreeNodeData
  def renameKey(keyName: String, newName: String): Unit
  def saveFile(path: SafePath, fileContent: String): Unit
  def saveFiles(fileContents: Seq[AlterableFileContent]): Unit

  //EXECUTIONS
  def allStates(lines: Int): (Seq[(ExecutionId, ExecutionInfo)], Seq[RunningOutputData])
  def staticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]
  def cancelExecution(id: ExecutionId): Unit
  def removeExecution(id: ExecutionId): Unit
  def runScript(scriptData: ScriptData): Unit
  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int, reset: Boolean): EnvironmentErrorData

  //MARKET
  def marketIndex(): MarketIndex
  def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit

  //PLUGINS
  def addPlugins(nodes: Seq[String]): Seq[Error]
  def autoAddPlugins(path: SafePath): Unit
  def isPlugin(path: SafePath): Boolean
  def allPluggableIn(path: SafePath): Seq[TreeNodeData]
  def listPlugins(): Iterable[Plugin]
  def removePlugin(plugin: Plugin): Unit

  //MODEL WIZARDS
  def launchingCommands(path: SafePath): Seq[LaunchingCommand]
  def models(archivePath: SafePath): Seq[SafePath]
  def classes(jarPath: SafePath): Seq[ClassTree]
  def methods(jarPath: SafePath, className: String): Seq[JarMethod]
  def buildModelTask(executableName: String, scriptName: String, command: String, language: Language, inputs: Seq[ProtoTypePair], outputs: Seq[ProtoTypePair], path: SafePath, imports: Option[String], libraries: Option[String], resources: Resources): TreeNodeData
  def expandResources(resources: Resources): Resources
}