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

  def deleteFile(treeNode: TreeNodeData): Unit

  def diff(subPath: SafePath, fullPath: SafePath): SafePath

  def fileSize(treeNodeData: TreeNodeData): Long

  def listFiles(path: TreeNodeData): Seq[TreeNodeData]

  def uuid(): String = java.util.UUID.randomUUID.toString

  def renameFileFromPath(filePath: SafePath, name: String): TreeNodeData

  def renameFile(treeNode: TreeNodeData, name: String): TreeNodeData

  def saveFile(path: String, fileContent: String): Unit

  def workspaceProjectNode(): TreeNodeData

  def authenticationKeysPath(): SafePath

  //EXECUTIONS
  def allExecutionStates(): Seq[(ExecutionId, ExecutionInfo)]

  def allSaticInfos(): Seq[(ExecutionId, StaticExecutionInfo)]

  def cancelExecution(id: ExecutionId): Unit

  def removeExecution(id: ExecutionId): Unit

  def runScript(scriptData: ScriptData): Unit

}