package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*

import scala.util.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.bootstrapnative.bsn.*
import org.openmole.gui.client.tool.bootstrapnative.Selector.Options
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
import io.github.nguyenyou.webawesome.laminar.*
import org.openmole.gui.shared.data.FirstLast.First

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

  val gitBranchList: Var[Option[BranchData]] = Var(None)


  val searchValue = Var("")
  val focused = Var(false)

  def filterTool(using api: ServerAPI, basePath: BasePath) =
    Input(
      _.value <-- searchValue.signal,
      _.width <-- focused.signal.map {
        case true => "200px"
        case false => "120px"
      },
      _.transition := "width 0.2s ease",
      _.onFocus.mapTo(true) --> focused,
      _.onBlur.mapTo(false) --> focused,
      _.placeholder := "Search",
      _.onKeyDown
        .filter(_.key == "Enter")
        .preventDefault --> { _ =>
        if searchValue.now() == ""
        then treeNodeManager.resetFileFinder
        treeNodeManager.find(searchValue.now())
      },
      _.onInput.mapToValue --> searchValue,
      _.marginLeft := "30px"
    )()

  def gitTools(using api: ServerAPI, basePath: BasePath) =
    val selectedVar = Var("")
    div(
      child <-- gitBranchList.signal.map: bdo =>
        bdo match
          case Some(bd: BranchData) =>
            //val curInd = bd.list.indexOf(bd.current)

            selectedVar.set(bd.current)

            def checkout(branch: String) =
              api.checkout(treeNodeManager.directory.now(), branch).andThen(_ =>
                treeNodePanel.refresh
              )

            val previous: Var[Option[String]] = Var(Some(bd.current))
            val branchItems =
              bd.list.map: b =>
                UOption(_.value := b)(b)

            Select(
              _.styleAttr :=
                """
                --wa-form-control-value-color: white;
                --wa-form-control-border-color: #795c85;
                --wa-form-control-background-color: #795c85;
              """,
              _.cls := "selectCaret",
              _.width := "170px",
              _.marginLeft := "10px",
              _.value <-- selectedVar,
              _.onInput.mapToValue --> { b =>
                selectedVar.set(b)
                println("SelectVar " + selectedVar.now())
                if !treeNodePanel.commitable.now().isEmpty
                then
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
              }
            )(
              branchItems *)

          case _ => emptyNode
    )

  def sortingGroup(using api: ServerAPI, basePath: BasePath) =

    val sortSelection: Var[String] = Var("Sort by name")

    div(marginLeft.auto,
      child <-- treeNodeManager.fileSorting.signal.map: fs =>
        def getCaret(fl: FirstLast) =
          fl match
            case FirstLast.First => Icon(_.name := "caret-up", _.marginLeft.auto)()
            case FirstLast.Last => Icon(_.name := "caret-down", _.marginLeft.auto)()

        def getIconAndName(ls: ListSorting, isItem: Boolean = true) =
          ls match
            case ListSorting.AlphaSorting =>
              if isItem
              then (span("name"), span("name"))
              else (span("  ", width := "20"), span("name", marginLeft := "5px"))
            case ListSorting.SizeSorting => (span(OMTags.glyph_data, width := "20"), span("size", marginLeft := "5px"))
            case ListSorting.TimeSorting => (span(OMTags.glyph_clock, width := "20"), span("update time", marginLeft := "5px"))

        def getItem(ls: ListSorting) = {
          val caret =
            if ls == fs.listSorting
            then getCaret(fs.firstLast)
            else div()
          val (icon, name) = getIconAndName(ls, false)

          DropdownItem(_.value := ls.toString)(
            div(display.flex, flexDirection.row, alignItems.center,
              icon,
              name,
              caret
            ))
        }

        val caretDirection = getCaret(fs.firstLast)
        val (icon, name) = getIconAndName(fs.listSorting)

        Dropdown(
          _.onSelect.map(_.detail.item.value.toOption) --> { sorting =>
            if Some(fs.listSorting.toString) == sorting
            then
              val reverse =
                fs.firstLast match
                  case FirstLast.First => FirstLast.Last
                  case FirstLast.Last => FirstLast.First
              treeNodeManager.fileSorting.set(fs.copy(firstLast = reverse))
            else
              sorting.foreach: s =>
                val sort = s match {
                  case "SizeSorting" => ListSorting.SizeSorting
                  case "TimeSorting" => ListSorting.TimeSorting
                  case _ => ListSorting.AlphaSorting
                }
                treeNodeManager.fileSorting.set(FileSorting(listSorting = sort))
          },
          _.slots.trigger(
            Button(
              _.marginLeft.auto,
            )(
              div(display.flex, flexDirection.row, alignItems.center,
                div("Sort by ", paddingRight := "5px"),
                icon,
                caretDirection
              )
            )
          ))(
          getItem(ListSorting.AlphaSorting),
          getItem(ListSorting.SizeSorting),
          getItem(ListSorting.TimeSorting)
        )
    )


