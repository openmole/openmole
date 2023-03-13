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

import org.openmole.gui.shared.data.{DataUtils, TreeNodeData, *}

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.Utils.*

import scala.concurrent.Future

sealed trait TreeNodeType {
  val uuid: String = DataUtils.uuID
  val name: String
}

trait DirNodeType extends TreeNodeType {
  val name: String = "Folder"
}

trait FileNodeType extends TreeNodeType {
  val name: String = "File"
}

object TreeNodeType {
  def file = new FileNodeType {}

  def folder = new DirNodeType {}
}

case class TreeNodeError(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit)

case class TreeNodeComment(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit)

sealed trait TreeNode {
  val id = DataUtils.uuID

  def name: String

  val size: Long

  val time: Long

}

def ListFiles(lfd: ListFilesData): TreeNode.ListFiles = lfd.map(TreeNode.treeNodeDataToTreeNode)

object TreeNode:

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode = tnd.directory match {
    case Some(dd: TreeNodeData.Directory) ⇒ Directory(tnd.name, tnd.size, tnd.time, dd.isEmpty)
    case _ ⇒ TreeNode.File(tnd.name, tnd.size, tnd.time, tnd.pluginState)
  }

  implicit def treeNodeToTreeNodeData(tn: TreeNode): TreeNodeData =
    val (dOf, pluginState) = tn match {
      case TreeNode.Directory(_, _, _, isEmpty) ⇒ (Some(TreeNodeData.Directory(isEmpty)), PluginState(false, false))
      case f: TreeNode.File ⇒ (None, f.pluginState)
    }

    TreeNodeData(tn.name, tn.size, tn.time, pluginState = pluginState, directory = dOf)

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
    isEmpty: Boolean) extends TreeNode

  case class File(
    name: String,
    size: Long,
    time: Long,
    pluginState: PluginState) extends TreeNode



