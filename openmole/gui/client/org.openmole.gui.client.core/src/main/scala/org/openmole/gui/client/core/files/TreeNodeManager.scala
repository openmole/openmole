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

import org.openmole.gui.client.core.files.FileToolBar.SelectedTool
import org.openmole.gui.client.core.{ CoreUtils, AlertPanel }
import org.openmole.gui.ext.data.SafePath
import rx._

package object treenodemanager {
  val instance = new TreeNodeManager

  def apply = instance
}

class TreeNodeManager {
  val root = DirNode(Var("projects"), Var(SafePath.sp(Seq("projects"))), 0L, "")

  val dirNodeLine: Var[Seq[DirNode]] = Var(Seq(root))

  val sons: Var[Map[DirNode, Seq[TreeNode]]] = Var(Map())

  val error: Var[Option[TreeNodeError]] = Var(None)

  val comment: Var[Option[TreeNodeComment]] = Var(None)

  val selectionMode: Var[Option[SelectedTool]] = Var(None)

  val selected: Var[Seq[TreeNode]] = Var(Seq())

  val copied: Var[Seq[TreeNode]] = Var(Seq())

  Obs(error) {
    error().map(AlertPanel.treeNodeErrorDiv)
  }

  Obs(comment) {
    comment().map(AlertPanel.treeNodeCommentDiv)
  }

  def computeAndGetCurrentSons = {
    computeCurrentSons()
    sons().get(current)
  }

  def trashCache(ontrashed: () ⇒ Unit) = CoreUtils.updateSons(current, ontrashed)

  def updateSon(dirNode: DirNode, newSons: Seq[TreeNode]) = sons() = sons().updated(dirNode, newSons)

  def isSelected(tn: TreeNode) = selected().contains(tn)

  def resetSelection = selected() = Seq()

  def setSelected(tn: TreeNode, b: Boolean) = b match {
    case true  ⇒ selected() = (selected() :+ tn).distinct
    case false ⇒ selected() = selected().filterNot(_ == tn)
  }

  def setSelectedAsCopied = {
    copied() = selected()
    selectionMode() = None

  }

  def emptyCopied = copied() = Seq()

  def setSelection(selectedTool: SelectedTool) = selectionMode() = selectionMode() match {
      case Some(s: SelectedTool) ⇒ if (s == selectedTool) None else Some(selectedTool)
      case _                     ⇒ Some(selectedTool)
    }

  def setFilesInError(question: String, files: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit) = error() = Some(TreeNodeError(question, files, okaction, cancelaction))

  def setFilesInComment(c: String, files: Seq[SafePath], okaction: () ⇒ Unit) = comment() = Some(TreeNodeComment(c, files, okaction))

  def noError = {
    error() = None
    comment() = None
  }

  def switchOffSelection = {
    selectionMode() = None
    resetSelection
  }

  def head = dirNodeLine().head

  def current = dirNodeLine().last

  def take(n: Int) = dirNodeLine().take(n)

  def drop(n: Int) = dirNodeLine().drop(n)

  def +(dn: DirNode) = dirNodeLine() = dirNodeLine() :+ dn

  def switch(dn: DirNode) =
    dirNodeLine() = dirNodeLine().zipWithIndex.filter(_._1 == dn).headOption.map {
      case (dn, index) ⇒ take(index + 1)
    }.getOrElse(dirNodeLine())

  def computeCurrentSons(oncomputed: () ⇒ Unit = () ⇒ {}): Unit = {
    current match {
      case dirNode: DirNode ⇒
        if (sons().contains(dirNode)) oncomputed()
        else CoreUtils.updateSons(dirNode, oncomputed)
      case _ ⇒
    }
  }

  def isRootCurrent = current == root

  def isProjectsEmpty = sons().getOrElse(root, Seq()).isEmpty

}
