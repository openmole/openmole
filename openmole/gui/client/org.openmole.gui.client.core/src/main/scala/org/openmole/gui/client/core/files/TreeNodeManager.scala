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
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data.{ FileFilter, ListFilesData, SafePath }
import rx._
import org.openmole.gui.client.core.files.TreeNode.ListFiles

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

package object treenodemanager {
  val instance = new TreeNodeManager

  def apply = instance
}

class TreeNodeManager {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val ROOTDIR = "projects"
  val root = SafePath.sp(Seq(ROOTDIR))

  val dirNodeLine: Var[SafePath] = Var(root)

  val sons: Var[Map[SafePath, ListFiles]] = Var(Map())

  val error: Var[Option[TreeNodeError]] = Var(None)

  val comment: Var[Option[TreeNodeComment]] = Var(None)

  val selected: Var[Seq[SafePath]] = Var(Seq())

  val copied: Var[Seq[SafePath]] = Var(Seq())

  val pluggables: Var[Seq[SafePath]] = Var(Seq())

  error.trigger {
    error.now.foreach(AlertPanel.treeNodeErrorDiv)
  }

  comment.trigger {
    comment.now.foreach(AlertPanel.treeNodeCommentDiv)
  }

  def isSelected(tn: TreeNode) = selected.now.contains(tn)

  def clearSelection = selected() = Seq()

  def clearSelectionExecpt(safePath: SafePath) = selected() = Seq(safePath)

  def setSelected(sp: SafePath, b: Boolean) = b match {
    case true  ⇒ selected() = (selected.now :+ sp).distinct
    case false ⇒ selected() = selected.now.filterNot(_ == sp)
  }

  def setSelectedAsCopied = copied() = selected.now

  def emptyCopied = copied() = Seq()

  def setFilesInError(question: String, files: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit) = error() = Some(TreeNodeError(question, files, okaction, cancelaction))

  def setFilesInComment(c: String, files: Seq[SafePath], okaction: () ⇒ Unit) = comment() = Some(TreeNodeComment(c, files, okaction))

  def noError = {
    error() = None
    comment() = None
  }

  val current = dirNodeLine

  def take(n: Int) = dirNodeLine.map { dn ⇒
    SafePath.sp(dn.path.take(n))
  }

  def drop(n: Int) = dirNodeLine.map { dn ⇒
    SafePath.sp(dn.path.drop(n))
  }

  def switch(dir: String): Unit = switch(dirNodeLine.now.copy(path = dirNodeLine.now.path :+ dir))

  def switch(sp: SafePath): Unit = {
    dirNodeLine() = sp
  }

  def invalidCurrentCache = invalidCache(current.now)

  def invalidCache(sp: SafePath) = sons() = sons.now.filterNot(_._1.path == sp.path)

  def computeCurrentSons(fileFilter: FileFilter): Future[ListFiles] = {
    val cur = current.now

    def getAndUpdateSons(safePath: SafePath): Future[ListFiles] = CoreUtils.getSons(safePath, fileFilter).map { newsons ⇒
      sons() = sons.now.updated(cur, newsons)
      newsons
    }

    cur match {
      case safePath: SafePath ⇒
        if (fileFilter.nameFilter.isEmpty) {
          if (sons.now.contains(safePath)) {
            Future(sons.now(safePath))
          }
          else {
            getAndUpdateSons(safePath)
          }
        }
        else getAndUpdateSons(safePath)
      case _ ⇒ Future(ListFilesData(Seq(), 0))
    }
  }

  def computePluggables(todo: () ⇒ Unit) = current.foreach { sp ⇒
    CoreUtils.pluggables(sp, todo)
  }

  def isRootCurrent = current.now == root

  def isProjectsEmpty = sons.now.getOrElse(root, ListFiles(Seq(), 0)).list.isEmpty

}
