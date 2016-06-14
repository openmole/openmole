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

import org.openmole.gui.ext.data._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.misc.utils.Utils._
import rx._

import scala.concurrent.Future

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

case class TreeNodeError(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit)

case class TreeNodeComment(message: String, filesInError: Seq[SafePath], okaction: () ⇒ Unit)

sealed trait TreeNode {
  val id = getUUID

  def name: Var[String]

  def safePath: Var[SafePath]

  val size: Long

  val readableSize: String

  val time: Long

  val readableTime: String

  def cloneWithName(newName: String) = FileNode(Var(newName), Var(SafePath.sp(safePath.now.path.dropRight(1) :+ newName)), size, readableSize, time, readableTime)

}

object TreeNode {

  implicit def treeNodeDataToTreeNode(tnd: TreeNodeData): TreeNode =
    if (tnd.isDirectory) DirNode(Var(tnd.name), Var(tnd.safePath), tnd.size, tnd.readableSize, tnd.time, tnd.readableTime)
    else FileNode(Var(tnd.name), Var(tnd.safePath), tnd.size, tnd.readableSize, tnd.time, tnd.readableTime)

  implicit def treeNodeToTreeNodeData(tn: TreeNode): TreeNodeData = TreeNodeData(tn.name.now, tn.safePath.now, tn match {
    case DirNode(_, _, _, _, _, _) ⇒ true
    case _                         ⇒ false
  }, tn.size, tn.readableSize, tn.time, tn.readableTime)

  implicit def seqTreeNodeToSeqTreeNodeData(tns: Seq[TreeNode]): Seq[TreeNodeData] = tns.map {
    treeNodeToTreeNodeData
  }

  implicit def futureSeqTreeNodeDataToFutureSeqTreeNode(ftnds: Future[Seq[TreeNodeData]]): Future[Seq[TreeNode]] = ftnds.map(seqTreeNodeDataToSeqTreeNode)

  implicit def seqTreeNodeDataToSeqTreeNode(tnds: Seq[TreeNodeData]): Seq[TreeNode] = tnds.map(treeNodeDataToTreeNode(_))

  implicit def treeNodeToSafePath(tn: TreeNode): SafePath = tn.safePath.now

  implicit def treeNodesToSafePaths(tns: Seq[TreeNode]): Seq[SafePath] = tns.map {
    treeNodeToSafePath
  }

  implicit def safePathToDirNode(safePath: SafePath) = DirNode(Var(safePath.name), Var(safePath), 0, "", 0, "")

}

case class DirNode(
  name:         Var[String],
  safePath:     Var[SafePath],
  size:         Long,
  readableSize: String,
  time:         Long,
  readableTime: String
) extends TreeNode

case class FileNode(
  name:         Var[String],
  safePath:     Var[SafePath],
  size:         Long,
  readableSize: String,
  time:         Long,
  readableTime: String
) extends TreeNode
