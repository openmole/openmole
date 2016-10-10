package org.openmole.gui.client.core.files

/*
 * Copyright (C) 24/07/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.{ CoreUtils, OMPost }
import org.openmole.gui.ext.data.{ FileFilter, SafePath }
import org.openmole.gui.shared.Api
import rx._
import autowire._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

package object treenodemanager {
  val instance = new TreeNodeManager

  def apply = instance
}

class TreeNodeManager {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val root = DirNode(Var("projects"), Var(SafePath.sp(Seq("projects"))), 0L, 0L)

  val dirNodeLine: Var[Seq[DirNode]] = Var(Seq(root))

  val sons: Var[Map[DirNode, Seq[TreeNode]]] = Var(Map())

  val error: Var[Option[TreeNodeError]] = Var(None)

  val comment: Var[Option[TreeNodeComment]] = Var(None)

  val selected: Var[Seq[TreeNode]] = Var(Seq())

  val copied: Var[Seq[TreeNode]] = Var(Seq())

  val pluggables: Var[Seq[TreeNode]] = Var(Seq())

  error.trigger {
    error.now.foreach(AlertPanel.treeNodeErrorDiv)
  }

  comment.trigger {
    comment.now.foreach(AlertPanel.treeNodeCommentDiv)
  }

  def updateSon(dirNode: DirNode, newSons: Seq[TreeNode]) = {
    sons() = sons.now.updated(dirNode, newSons)
  }

  def isSelected(tn: TreeNode) = selected.now.contains(tn)

  def clearSelection = selected() = Seq()

  def clearSelectionExecpt(tn: TreeNode) = selected() = Seq(tn)

  def setSelected(tn: TreeNode, b: Boolean) = b match {
    case true  ⇒ selected() = (selected.now :+ tn).distinct
    case false ⇒ selected() = selected.now.filterNot(_ == tn)
  }

  def setSelectedAsCopied = copied() = selected.now

  def emptyCopied = copied() = Seq()

  def setFilesInError(question: String, files: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit) = error() = Some(TreeNodeError(question, files, okaction, cancelaction))

  def setFilesInComment(c: String, files: Seq[SafePath], okaction: () ⇒ Unit) = comment() = Some(TreeNodeComment(c, files, okaction))

  def noError = {
    error() = None
    comment() = None
  }

  val head = dirNodeLine.map {
    _.head
  }

  val current = dirNodeLine.map {
    _.last
  }

  def take(n: Int) = dirNodeLine.map {
    _.take(n)
  }

  def drop(n: Int) = dirNodeLine.map {
    _.drop(n)
  }

  def +(dn: DirNode) = dirNodeLine() = dirNodeLine.now :+ dn

  def switch(dn: DirNode) = {
    dirNodeLine() = dirNodeLine.now.zipWithIndex.filter { d ⇒ d._1 == dn }.headOption.map {
      case (dn, id) ⇒ take(id + 1)
    }.getOrElse(dirNodeLine).now
  }

  def computeCurrentSons(fileFilter: FileFilter): Future[(Seq[TreeNode], Boolean)] = {
    current.now match {
      case dirNode: DirNode ⇒
        if (sons.now.contains(dirNode)) {
          OMPost[Api].resetMoreEntriesBuffer(dirNode.safePath.now).call()
          Future((sons.now(dirNode), false))
        }
        else CoreUtils.getSons(dirNode, fileFilter).map { x ⇒ (x, true) }
      case _ ⇒ Future(Seq(), false)
    }
  }

  def computePluggables(todo: () ⇒ Unit) = current.now match {
    case dirNode: DirNode ⇒ CoreUtils.pluggables(dirNode, todo)
    case _                ⇒
  }

  def isRootCurrent = current.now == root

  def isProjectsEmpty = sons.now.getOrElse(root, Seq()).isEmpty

}
