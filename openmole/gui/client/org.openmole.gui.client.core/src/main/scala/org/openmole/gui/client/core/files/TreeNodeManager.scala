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

class TreeNodeManager:

  val root = SafePath.root(ServerFileSystemContext.Project)
  
  val directory: Var[SafePath] = Var(root)

  //val sons: Var[Map[SafePath, ListFiles]] = Var(Map())

  val selected: Var[Seq[SafePath]] = Var(Seq())

  val copied: Var[Seq[SafePath]] = Var(Seq())

  val fileSorting = Var(FileSorting())

  val findFilesContaining: Var[(Option[String], Seq[(SafePath, Boolean)])] = Var((None, Seq()))

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

  def switch(dir: String): Unit = switch(directory.now() / dir)
  def switch(sp: SafePath): Unit =
    updateFilter(FileSorting())
    directory.set(sp)

  def goToHome = switch(root)
    
  def updateFilter(newFilter: FileSorting) = fileSorting.set(newFilter)


  def switchSorting(fileSorting: FileSorting, newListSorting: ListSorting) =
    val fl =
      if fileSorting.fileSorting == newListSorting
      then
        fileSorting.firstLast match
          case FirstLast.First ⇒ FirstLast.Last
          case _ ⇒ FirstLast.First
      else FirstLast.First

    fileSorting.copy(fileSorting = newListSorting, firstLast = fl)

  def switchAlphaSorting =
    updateFilter(switchSorting(fileSorting.now(), ListSorting.AlphaSorting))

  def switchTimeSorting =
    updateFilter(switchSorting(fileSorting.now(), ListSorting.TimeSorting))

  def switchSizeSorting =
    updateFilter(switchSorting(fileSorting.now(), ListSorting.SizeSorting))

    //directory.update(identity)//set(directory.now())
    //invalidCache(directory.now())

//  def invalidCache(sp: SafePath)(using api: ServerAPI, path: BasePath) = {
//    sons.update(_.filterNot(_._1.path == sp.path))
//    computeCurrentSons
//  }

//  def computeCurrentSons(using api: ServerAPI, path: BasePath) =
//    val cur = directory.now()
//
//    def updateSons(safePath: SafePath) =
//      CoreUtils.listFiles(safePath, fileFilter.now()).foreach { lf => sons.update { s => s.updated(cur, ListFiles(lf)) } }
//
//    cur match
//      case safePath: SafePath ⇒ if !sons.now().contains(safePath) then updateSons(safePath)
//      case _ ⇒ Future(ListFilesData.empty)


  def resetFileFinder = findFilesContaining.set((None, Seq()))

  def find(findString: String)(using api: ServerAPI, path: BasePath) = {
    def updateSearch = {
      val safePath: SafePath = directory.now()
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

