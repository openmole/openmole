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

import org.openmole.gui.ext.data.{ SafePath, TreeNodeData }
import TreeNodeTabs.TreeNodeTab
import org.openmole.gui.misc.utils.Utils._
import rx._

sealed trait TreeNodeType {
  val uuid: String = getUUID
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

sealed trait TreeNode {
  val id = getUUID

  def name: Var[String]

  def safePath: Var[SafePath]

  def hasSons: Boolean

  val size: Long

  val readableSize: String

  def isPlugin: Boolean
}

object TreeNode {

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode =
    if (tnd.isDirectory) DirNode(tnd.name, tnd.safePath, tnd.size, tnd.readableSize, tnd.isPlugin, Var(Seq()))
    else FileNode(tnd.name, tnd.safePath, tnd.size, tnd.isPlugin, tnd.readableSize)

  implicit def treeNodeToTreeNodeData(tn: TreeNode): TreeNodeData = TreeNodeData(tn.name(), tn.safePath(), tn match {
    case DirNode(_, _, _, _, _, _) ⇒ true
    case _                         ⇒ false
  }, tn.isPlugin, tn.size, tn.readableSize)

  implicit def seqTreeNodeToSeqTreeNodeData(tns: Seq[TreeNode]): Seq[TreeNodeData] = tns.map {
    treeNodeToTreeNodeData
  }

  implicit def seqTreeNodeDataToSeqTreeNode(tnds: Seq[TreeNodeData]): Seq[TreeNode] = tnds.map(treeNodeDataToTreeNode(_))

  implicit def safePathToPartialTreeNodeData(sPath: SafePath): TreeNodeData = TreeNodeData(sPath.name, sPath.parent, false, false, 0L, "")

  implicit def treeNodeToSafePath(tn: TreeNode): SafePath = tn.safePath()

  implicit def treeNodesToSafePaths(tns: Seq[TreeNode]): Seq[SafePath] = tns.map { treeNodeToSafePath }

  def fromFilePath(path: SafePath) = FileNode()

}

object TreeNodeOrdering extends Ordering[TreeNode] {
  def compare(tn1: TreeNode, tn2: TreeNode) = tn1 match {
    case dn1: DirNode ⇒ tn2 match {
      case dn2: DirNode ⇒ dn1.name() compare dn2.name()
      case _            ⇒ -1
    }
    case _ ⇒ tn2 match {
      case dn2: DirNode ⇒ 1
      case _            ⇒ tn1.name() compare tn2.name()
    }
  }
}

case class DirNode(name: Var[String],
                   safePath: Var[SafePath],
                   size: Long,
                   readableSize: String,
                   isPlugin: Boolean = false,
                   sons: Var[Seq[TreeNode]] = Var(Seq())) extends TreeNode {
  def hasSons = sons().size > 0
}

case class FileNode(name: Var[String],
                    safePath: Var[SafePath],
                    size: Long,
                    isPlugin: Boolean = false,
                    readableSize: String) extends TreeNode {
  def hasSons = false
}

