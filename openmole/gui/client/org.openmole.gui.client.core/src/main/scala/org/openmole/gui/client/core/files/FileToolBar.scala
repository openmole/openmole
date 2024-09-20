package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*

import scala.util.*
import com.raquo.laminar.api.L.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.ext.*

import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.openmole.gui.client.core.Waiter.*
import org.openmole.gui.client.tool.*
import org.openmole.gui.client.core.*
import org.openmole.gui.client.ext.FileManager
import org.openmole.gui.shared.api.*

/*
 * Copyright (C) 20/01/16 // mathieu.leclaire@openmole.org
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

class FileToolBar(treeNodePanel: TreeNodePanel, treeNodeManager: TreeNodeManager):
  def manager = treeNodePanel.treeNodeManager

  val findInput = inputTag("").amend(
    width := "180px",
    marginTop := "12px",
    onMountFocus
  )

  val gitFolder: Var[Boolean] = Var(false)

  val filterToolOpen = Var(false)

  def filterTool(using api: ServerAPI, basePath: BasePath) = div(
    cls := "file-filter",
    //  label("# of entries ", width := "30px", margin := "0 15 0 10"),
    // form(thresholdInput, onSubmit.preventDefault --> { _ ⇒ filterSubmit }),
    label("Find ", width := "30px", margin := "0 15 0 10"),
    form(findInput, onMountFocus, onSubmit.preventDefault --> { _ ⇒ treeNodeManager.find(findInput.ref.value) }),
    div(cls := "close-button bi-x", onClick --> { _ =>
      filterToolOpen.set(false)
      findInput.ref.value = ""
      treeNodeManager.resetFileFinder
    })
  )

  def sortingGroup(using api: ServerAPI, basePath: BasePath) =
    def item(sorting: ListSorting) =
      div(
        centerInDiv,
        div(
          sorting match
            case ListSorting.AlphaSorting ⇒ "Aa"
            case ListSorting.TimeSorting ⇒ OMTags.glyph_clock
            case ListSorting.SizeSorting ⇒ OMTags.glyph_data
          ,
          cls <-- treeNodeManager.fileSorting.signal.map: s =>
            if s.fileSorting == sorting
            then "sorting-files-item-selected"
            else "sorting-files-item"
        ),
        onClick --> { _ ⇒
          val currentSorting = treeNodeManager.fileSorting.now()
          if currentSorting.fileSorting == sorting
          then
            val reverse =
              currentSorting.firstLast match
                case FirstLast.First => FirstLast.Last
                case FirstLast.Last => FirstLast.First
            treeNodeManager.fileSorting.set(currentSorting.copy(firstLast = reverse))
          else treeNodeManager.fileSorting.set(FileSorting(fileSorting = sorting))
        }
      )

    div(
      centerInDiv, backgroundColor := "#3f3d56",
      div(cls <-- gitFolder.signal.map(gf=> if gf then "specific-file git gitInfo" else "")),
      div(flexRow, justifyContent.right,
        div(
          cls <-- filterToolOpen.signal.map { o =>
            if (o) "open-transition" else "close-transition"
          },
          filterTool
        )),
        div(
          cls := "sorting-files",
          children <-- treeNodeManager.fileSorting.signal.map: fs ⇒
            Seq(
              item(ListSorting.AlphaSorting),
              item(ListSorting.TimeSorting),
              item(ListSorting.SizeSorting),
              div(
                cls := "sorting-file-item-caret",
                marginTop := "4",
                fs.firstLast match
                  case FirstLast.Last ⇒ glyph_triangle_up
                  case FirstLast.First ⇒ glyph_triangle_down
              )
            )

        )
      )


