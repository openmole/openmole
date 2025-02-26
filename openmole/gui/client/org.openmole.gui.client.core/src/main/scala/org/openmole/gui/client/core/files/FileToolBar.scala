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
import org.openmole.gui.client.core.files.TreeNodePanel.MultiTool.{Git, On}
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

  val filterToolOpen = Var(false)

  lazy val findInput = inputTag("").amend(
    width := "180px",
    marginTop := "12px",
    inContext{ctx =>
      filterToolOpen.signal.toObservable --> Observer[Boolean] { e => if e then ctx.ref.focus()}
    },
  )

  val gitBranchList: Var[Option[BranchData]] = Var(None)


  def filterTool(using api: ServerAPI, basePath: BasePath) = div(
    cls := "file-filter",
    //  label("# of entries ", width := "30px", margin := "0 15 0 10"),
    // form(thresholdInput, onSubmit.preventDefault --> { _ => filterSubmit }),
    label("Find ", width := "30px", margin := "0 10 0 10"),
    form(findInput, onMountFocus, onSubmit.preventDefault --> { _ => treeNodeManager.find(findInput.ref.value) })
  )

  def sortingGroup(using api: ServerAPI, basePath: BasePath) =
    def item(sorting: ListSorting) =
      div(
        centerInDiv,
        div(
          sorting match
            case ListSorting.AlphaSorting => "Aa"
            case ListSorting.TimeSorting => OMTags.glyph_clock
            case ListSorting.SizeSorting => OMTags.glyph_data
          ,
          cls <-- treeNodeManager.fileSorting.signal.map: s =>
            if s.fileSorting == sorting
            then "sorting-files-item-selected"
            else "sorting-files-item"
        ),
        onClick --> { _ =>
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
      display.flex, justifyContent.spaceBetween, width := "150", backgroundColor := "#3f3d56",
      div(
        child <-- gitBranchList.signal.map: bdo =>
          bdo match
            case Some(bd: BranchData) =>
              val curInd = bd.list.indexOf(bd.current)

              def checkout(branch: String) =
                api.checkout(treeNodeManager.directory.now(), branch).andThen(_ =>
                  treeNodePanel.refresh
                )
              val previous: Var[Option[String]] = Var(Some(bd.current))
              lazy val opts: Options[String] = bd.list.options(
                curInd,
                Seq(cls := "btn btn-purple", minWidth := "90", marginLeft := "10"),
                (m: String) => m,
                onclickExtra = ()=> previous.set(opts.content.now()),
                onclose = () =>
                  opts.get.foreach: b =>
                    if !treeNodePanel.commitable.now().isEmpty
                    then
                      previous.now().foreach(p=> opts.set(p))
                      treeNodePanel.multiTool.set(Git)
                      treeNodePanel.confirmationDiv.set(
                        Some(treeNodePanel.confirmation(s"Modifications pending, stash or commit your changes.", "Stash", () =>
                          api.stash(treeNodeManager.directory.now()).andThen { _ =>
                            checkout(b)
                            treeNodePanel.closeMultiTool
                          }
                        ))
                      )
                    else checkout(b)
              )
              opts.selector
            case _ => emptyNode
      ),
      div(
        cls := "sorting-files",
        children <-- treeNodeManager.fileSorting.signal.map: fs =>
          Seq(
            item(ListSorting.AlphaSorting),
            item(ListSorting.TimeSorting),
            item(ListSorting.SizeSorting),
            div(
              cls := "sorting-file-item-caret",
              marginTop := "4",
              fs.firstLast match
                case FirstLast.Last => glyph_triangle_up
                case FirstLast.First => glyph_triangle_down
            )
          )

      )
    )


