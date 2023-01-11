package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.util._
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.client._

import scala.concurrent.ExecutionContext.Implicits.global
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.raw.{HTMLButtonElement, HTMLElement, HTMLInputElement, HTMLSpanElement}
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{FileZone, RelativeCenterPosition}
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.tool._
import org.openmole.gui.client.core._
import org.openmole.gui.ext.client.FileManager

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

class FileToolBar(treeNodePanel: TreeNodePanel, treeNodeManager: TreeNodeManager) {
  def manager = treeNodePanel.treeNodeManager

  val message = Var[Div](div())

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  // Filter tool
  val nameTag = "names"
  val thresholdChanged = Var(false)

  val findInput = inputTag("").amend(
    width := "180px",
    marginTop := "12px",
    onMountFocus
  )

  val filterToolOpen = Var(false)

  def filterTool(using fetch: Fetch) = div(
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

  //  val pluginButton =
  //    button(
  //      "Plug",
  //      btn_secondary,
  //      onClick --> { _ ⇒
  //        val directoryName = s"uploadPlugin${java.util.UUID.randomUUID().toString}"
  //        Post()[Api].copyToPluginUploadDir(directoryName, manager.selected.now).call().foreach { _ ⇒
  //          import scala.concurrent.duration._
  //          val names = manager.selected.now.map(_.name)
  //          Post(timeout = 5 minutes)[Api].addUploadedPlugins(directoryName, names).call().foreach {
  //            errs ⇒
  //              if (errs.isEmpty) pluginPanel.pluginDialog.show
  //              else panels.alertPanel.detail("Plugin import failed", ErrorData.stackTrace(errs.head), transform = RelativeCenterPosition, zone = FileZone)
  //          }
  //        //  unselectToolAndRefreshTree
  //        }
  //      }
  //    )

  //Filter
  implicit def stringToIntOption(s: String): Option[Int] = Try(s.toInt).toOption

  def sortingGroup(using fetch: Fetch) = {
    trait Sorting
    object Name extends Sorting
    object Size extends Sorting
    object Time extends Sorting

    trait State
    object Up extends State
    object Down extends State

    case class SortingState(sorting: Sorting, state: State)
    val sortingState: Var[SortingState] = Var(SortingState(Name, Up))

    def item(sorting: Sorting, sState: SortingState) = {
      val isSelected = sorting == sState.sorting
      div(
        centerInDiv,
        div(
          sorting match {
            case Name ⇒ "Aa"
            case Time ⇒ OMTags.glyph_clock
            case Size ⇒ OMTags.glyph_data
          },
          cls := "sorting-files-item" + {
            if (isSelected) "-selected" else ""
          }
        ),
        onClick --> { _ ⇒
          sortingState.update(ss ⇒ SortingState(
            sorting,
            if (isSelected) {
              if (ss.state == Up) Down
              else Up
            }
            else Up))
          sorting match {
            case Name ⇒ manager.switchAlphaSorting
            case Size ⇒ manager.switchSizeSorting
            case Time ⇒ manager.switchTimeSorting
          }
        }
      )
    }

    div(
      centerInDiv, backgroundColor := "#3f3d56",
      div(flexRow, justifyContent.right,
        div(
          cls <-- filterToolOpen.signal.map { o =>
            if (o) "open-transition" else "close-transition"
          },
          filterTool
        )),
        div(
          cls := "sorting-files",
          children <-- sortingState.signal.map { ss ⇒
            Seq(
              item(Name, ss),
              item(Time, ss),
              item(Size, ss),
              div(
                cls := "sorting-file-item-caret",
                ss.state match {
                  case Up ⇒ glyph_triangle_up
                  case Down ⇒ glyph_triangle_down
                }
              )
            )
          }
        )
      )

  }

}
