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
  //AUTHENTICATIONS
  def authentications(): Seq[AuthenticationData]

  def addAuthentication(data: AuthenticationData): Unit

  def removeAuthentication(data: AuthenticationData): Unit

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean

  def passwordState(): PasswordState

  def resetPassword(): Unit

  def setPassword(pass: String): Boolean

  //FILES
  def addDirectory(treeNode: TreeNodeData, directoryName: String): Boolean

  def addFile(treeNode: TreeNodeData, fileName: String): Boolean

  def extractTGZ(treeNodeData: TreeNodeData): Unit

  def deleteAuthenticationKey(keyName: String): Unit

  def deleteFile(safePath: SafePath): Unit

  def exists(safePath: SafePath): Boolean

  def fileSize(treeNodeData: TreeNodeData): Long

  def listFiles(path: TreeNodeData): Seq[TreeNodeData]

  def mdToHtml(safePath: SafePath): String

  def move(from: SafePath, to: SafePath): Unit

  def uuid(): String = java.util.UUID.randomUUID.toString

  def renameFileFromPath(filePath: SafePath, name: String): TreeNodeData

  def renameFile(treeNode: TreeNodeData, name: String): TreeNodeData

  def renameKey(keyName: String, newName: String): Unit

  def saveFile(path: SafePath, fileContent: String): Unit

  def saveFiles(fileContents: Seq[AlterableFileContent]): Unit

  def workspaceProjectNode(): SafePath

  def authenticationKeysPath(): SafePath

  //EXECUTIONS
  def allStates(): Seq[(ExecutionId, StaticExecutionInfo, ExecutionInfo)]

  def cancelExecution(id: ExecutionId): Unit

  def removeExecution(id: ExecutionId): Unit

  def runScript(scriptData: ScriptData): Unit

  def runningErrorEnvironmentAndOutputData(lines: Int): (Seq[RunningEnvironmentData], Seq[RunningOutputData])

  //INFO
  def buildInfo: OpenMOLEBuildInfo

  //MARKET
  def marketIndex: MarketIndex
  def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Unit

}