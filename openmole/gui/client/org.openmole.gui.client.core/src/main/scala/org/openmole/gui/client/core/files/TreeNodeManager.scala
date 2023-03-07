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

import org.openmole.gui.client.core.{CoreUtils, CoreFetch, Panels}
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.core.files.TreeNode.ListFiles
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import scalaz.Success

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TreeNodeManager {

  val root = SafePath.empty
  
  val dirNodeLine: Var[SafePath] = Var(root)

  val sons: Var[Map[SafePath, ListFiles]] = Var(Map())

  val error: Var[Option[TreeNodeError]] = Var(None)

  val comment: Var[Option[TreeNodeComment]] = Var(None)

  val selected: Var[Seq[SafePath]] = Var(Seq())

  val copied: Var[Seq[SafePath]] = Var(Seq())

  val pluggables: Var[Seq[SafePath]] = Var(Seq())

  val fileFilter = Var(FileFilter.defaultFilter)

  val findFilesContaining: Var[(Option[String], Seq[(SafePath, Boolean)])] = Var((None, Seq()))

  def errorObserver(using panels: Panels) = Observer[Option[TreeNodeError]] { err ⇒
    err.foreach(panels.alertPanel.treeNodeErrorDiv)
  }

  def commentObserver(using panels: Panels) = Observer[Option[TreeNodeComment]] { tnc ⇒
    tnc.foreach(panels.alertPanel.treeNodeCommentDiv)
  }

  def isSelected(tn: TreeNode) = selected.now().contains(tn)

  def clearSelection = selected.set(Seq())

  def clearSelectionExecpt(safePath: SafePath) = selected.set(Seq(safePath))

  def setSelected(sp: SafePath, b: Boolean) = {
    b match {
      case true ⇒ selected.update(s ⇒ (s :+ sp).distinct)
      case false ⇒ selected.update(s ⇒ s.filterNot(_ == sp))
    }
  }

  def switchSelection(sp: SafePath) = {
    selected.update(s => s.contains(sp) match {
      case true => s.filterNot(_ == sp)
      case _ => (s :+ sp).distinct
    }
    )
  }

  def switchAllSelection(safePaths: Seq[SafePath], b: Boolean) = safePaths.map { f => setSelected(f, b) }

  def setSelectedAsCopied = copied.set(selected.now())

  def emptyCopied = copied.set(Seq())

  def setFilesInError(question: String, files: Seq[SafePath], okaction: () ⇒ Unit, cancelaction: () ⇒ Unit) = error.set(Some(TreeNodeError(question, files, okaction, cancelaction)))

  def setFilesInComment(c: String, files: Seq[SafePath], okaction: () ⇒ Unit) = comment.set(Some(TreeNodeComment(c, files, okaction)))

  def noError = {
    error.set(None)
    comment.set(None)
  }

  def switch(dir: String): Unit = switch(dirNodeLine.now().copy(path = dirNodeLine.now().path :+ dir))

  def switch(sp: SafePath): Unit = dirNodeLine.set(sp)

  def updateFilter(newFilter: FileFilter) = fileFilter.set(newFilter)

  def switchAlphaSorting(using api: ServerAPI, path: BasePath) =
    updateFilter(fileFilter.now().switchTo(ListSorting.AlphaSorting))
    invalidCurrentCache

  def switchTimeSorting(using api: ServerAPI, path: BasePath) =
    updateFilter(fileFilter.now().switchTo(ListSorting.TimeSorting))
    invalidCurrentCache

  def switchSizeSorting(using api: ServerAPI, path: BasePath) =
    updateFilter(fileFilter.now().switchTo(ListSorting.SizeSorting))
    invalidCurrentCache

  def invalidCurrentCache(using api: ServerAPI, path: BasePath) = invalidCache(dirNodeLine.now())

  def invalidCache(sp: SafePath)(using api: ServerAPI, path: BasePath) = {
    sons.update(_.filterNot(_._1.path == sp.path))
    computeCurrentSons
  }

  def computeCurrentSons(using api: ServerAPI, path: BasePath) = {
    val cur = dirNodeLine.now()

    def updateSons(safePath: SafePath) = {
      CoreUtils.listFiles(safePath, fileFilter.now()).foreach { lf =>
        sons.update { s => s.updated(cur, ListFiles(lf)) }
      }
    }

    cur match {
      case safePath: SafePath ⇒
        if (!sons.now().contains(safePath))
          updateSons(safePath)
      case _ ⇒ Future(ListFilesData.empty)
    }
  }

  def resetFileFinder = findFilesContaining.set((None, Seq()))

  def find(findString: String)(using api: ServerAPI, path: BasePath) = {
    def updateSearch = {
      val safePath: SafePath = dirNodeLine.now()
      CoreUtils.findFilesContaining(safePath, Some(findString)).foreach { fs =>
        findFilesContaining.set((Some(findString), fs))
      }
    }

    if (!findString.isEmpty) {
      findFilesContaining.now() match {
        case (Some(fs), _) if (fs != findString) => updateSearch
        case (None, _) => updateSearch
      }
    }
  }

}
