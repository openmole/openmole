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
  //FILES
  def addDirectory(treeNode: TreeNodeData, directoryName: String): Boolean

  def addFile(treeNode: TreeNodeData, fileName: String): Boolean

  def deleteFile(treeNode: TreeNodeData): Unit

  def fileSize(treeNodeData: TreeNodeData): Long

  def listFiles(path: TreeNodeData): Seq[TreeNodeData]

  def uuid(): String = java.util.UUID.randomUUID.toString

  def renameFile(treeNode: TreeNodeData, name: String): Boolean

  def saveFile(path: String, fileContent: String): Unit

  def workspacePath(): String

  //EXECUTIONS
  def allExecutionStates(): Seq[(ExecutionId, ExecutionInfo)]

  def cancelExecution(id: ExecutionId): Unit

  def runScript(scriptData: ScriptData): String
}