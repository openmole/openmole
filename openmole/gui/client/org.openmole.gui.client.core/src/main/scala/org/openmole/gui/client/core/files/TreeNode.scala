package org.openmole.gui.client.core.files

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.shared.data.*

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.ClientUtil.*

import scala.concurrent.Future

enum TreeNodeType(name: String):
  val uuid: String = randomId
  case File extends TreeNodeType("File")
  case Folder extends TreeNodeType("Folder")

case class TreeNodeError(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit)

case class TreeNodeComment(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit)

sealed trait TreeNode:
  val id = randomId
  def name: String
  val size: Long
  val time: Long
  val gitStatus: Option[GitStatus]

def ListFiles(lfd: FileListData): TreeNode.ListFiles = lfd.data.map(TreeNode.treeNodeDataToTreeNode)

object TreeNode:

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode = tnd.directory match {
    case Some(dd: TreeNodeData.Directory) ⇒ Directory(tnd.name, tnd.size, tnd.time, dd.isEmpty)
    case _ ⇒ TreeNode.File(tnd.name, tnd.size, tnd.time, tnd.pluginState, tnd.gitStatus)
  }

  implicit def treeNodeToTreeNodeData(tn: TreeNode): TreeNodeData =
    val (dOf, pluginState) = tn match {
      case TreeNode.Directory(_, _, _, isEmpty,_) ⇒ (Some(TreeNodeData.Directory(isEmpty)), PluginState(false, false))
      case f: TreeNode.File ⇒ (None, f.pluginState)
    }

    TreeNodeData(tn.name, tn.size, tn.time, pluginState = pluginState, directory = dOf, tn.gitStatus)

  implicit def seqTreeNodeToSeqTreeNodeData(tns: Seq[TreeNode]): Seq[TreeNodeData] = tns.map { treeNodeToTreeNodeData }
  implicit def futureSeqTreeNodeDataToFutureSeqTreeNode(ftnds: Future[Seq[TreeNodeData]]): Future[Seq[TreeNode]] = ftnds.map(seqTreeNodeDataToSeqTreeNode)
  implicit def seqTreeNodeDataToSeqTreeNode(tnds: Seq[TreeNodeData]): Seq[TreeNode] = tnds.map(treeNodeDataToTreeNode(_))

  type ListFiles = Seq[TreeNode]


  def isDir(node: TreeNode) =
    node match
      case _: Directory => true
      case _ => false

  case class Directory(
    name: String,
    size: Long,
    time: Long,
    isEmpty: Boolean,
    gitStatus: Option[GitStatus] = None) extends TreeNode

  case class File(
    name: String,
    size: Long,
    time: Long,
    pluginState: PluginState,
    gitStatus: Option[GitStatus]) extends TreeNode



