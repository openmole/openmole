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

import org.openmole.gui.ext.data.TreeNodeData
import org.openmole.gui.misc.utils.Utils._
import rx._

object NodeState extends Enumeration {

  case class NodeState(state: String) extends Val(state)

  val EXPANDED = new NodeState("EXPANDED")
  val COLLAPSED = new NodeState("COLLAPSED")
  val FILE = new NodeState("FILE")
}

import NodeState._

sealed trait TreeNode {
  def name: Var[String]

  def canonicalPath: Var[String]

  def state: Var[NodeState]

  def hasSons: Boolean
}

object TreeNode {

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode =
    if (tnd.isDirectory) DirNode(tnd.name, tnd.canonicalPath, Var(Seq()))
    else FileNode(tnd.name, tnd.canonicalPath)

  implicit def treeNodeToTreeNodeData(tn: TreeNode): TreeNodeData = TreeNodeData(tn.name(), tn.canonicalPath(), tn match {
    case DirNode(_, _, _, _) ⇒ true
    case _                   ⇒ false
  })

  implicit def seqTreeNodeToSeqTreeNodeData(tns: Seq[TreeNode]): Seq[TreeNodeData] = tns.map {
    treeNodeToTreeNodeData
  }

  implicit def seqTreeNodeDataToSeqTreeNode(tnds: Seq[TreeNodeData]): Seq[TreeNode] = tnds.map(treeNodeDataToTreeNode(_))

  implicit def oo(s: Seq[(TreeNodeData, Seq[TreeNodeData])]): Seq[(TreeNode, Seq[TreeNode])] = s.map { tu ⇒
    (treeNodeDataToTreeNode(tu._1), seqTreeNodeDataToSeqTreeNode(tu._2))
  }
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
                   canonicalPath: Var[String],
                   sons: Var[Seq[TreeNode]] = Var(Seq()), state: Var[NodeState] = Var(COLLAPSED)) extends TreeNode {
  def hasSons = sons().size > 0
}

case class FileNode(name: Var[String],
                    canonicalPath: Var[String]) extends TreeNode {
  val hasSons = false
  val state = Var(FILE)
}
