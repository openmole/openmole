package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.util._
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.CoreUtils
import autowire._
import org.openmole.gui.ext.client._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.raw.{HTMLButtonElement, HTMLElement, HTMLInputElement, HTMLSpanElement}
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{FileZone, RelativeCenterPosition}
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.panels._
import org.openmole.gui.client.tool._
import org.openmole.gui.ext.api.Api
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

object FileToolBar {

  trait SelectedTool {
    def glyph: Setter[ReactiveHtmlElement.Base]
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
  }

  object FileCreationTool extends SelectedTool {
    val glyph = glyph_plus
  }

  object PluginTool extends SelectedTool {
    val glyph = OMTags.glyph_plug
  }

  object CopyTool extends SelectedTool {
    val glyph = glyph_copy
  }

  object PasteTool extends SelectedTool {
    val glyph = glyph_paste
  }

  object RefreshTool extends SelectedTool {
    val glyph = glyph_refresh
  }

}

import FileToolBar._

class FileToolBar(treeNodePanel: TreeNodePanel) {
  def manager = treeNodePanel.treeNodeManager

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val fileFilter = Var(FileFilter.defaultFilter)
  val message = Var[Div](div())
  val fileNumberThreshold = 1000

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  def clearMessage = message.set(div())

  def buildSpan(tool: SelectedTool, legend: String, todo: () ⇒ Unit, modifierSeq: HESetters = emptySetters) = {
    span(
      tool.glyph, cursor.pointer, modifierSeq,
      cls := "glyphmenu",
      onClick --> { _ ⇒ todo() }
    ).tooltip(legend)
  }

  def buildAndSelectSpan(tool: SelectedTool, legend: String, todo: Boolean ⇒ Unit = (Boolean) ⇒ {}) =
    buildSpan(tool, legend, { () ⇒
      val isSelectedTool = selectedTool.now == Some(tool)
      tool match {
        case CopyTool | TrashTool ⇒ treeNodePanel.turnSelectionTo(true)
        case PluginTool ⇒
          manager.computePluggables(() ⇒
            if (manager.pluggables.now.isEmpty)
              message.set(div(color := WHITE, "No plugin could be found in this folder."))
            else {
              clearMessage
              treeNodePanel.turnSelectionTo(true)
            })
        case _ ⇒
      }
      selectedTool.set(Some(tool))

      // todo(isSelectedTool)
    })

  // Filter tool
  val thresholdTag = "threshold"
  val nameTag = "names"
  val thresholdChanged = Var(false)

  val thresholdInput = inputTag(fileNumberThreshold.toString).amend(
    width := "55px",
    onMountFocus,
    cls := "form-control", marginTop := "12px", padding := "10px",
    cls.toggle("colorTransition") <-- thresholdChanged.signal
  )

  val nameInput = inputTag("").amend(
    width := "60px",
    marginTop := "12px",
    onMountFocus
  )

  def updateFilter: Unit = updateFilter(fileFilter.now.copy(threshold = thresholdInput.ref.value, nameFilter = nameInput.ref.value))

  def resetFilterThresold = {
    thresholdInput.ref.value = "1000"
    thresholdChanged.set(true)
  }

  def resetFilterTools: Unit = {
    Try {
      val th = thresholdInput.ref.value.toInt
      if (th > 1000 || thresholdInput.ref.value == "") resetFilterThresold
      else thresholdChanged.set(false)
    } match {
      case Failure(exception) ⇒
        resetFilterThresold
        resetFilterTools
      case Success(_) ⇒
        updateFilter(fileFilter.now.copy(threshold = thresholdInput.ref.value, nameFilter = nameInput.ref.value))
    }

  }

  def filterSubmit {
    resetFilterTools
    treeNodePanel.invalidCacheAndDraw
  }

  val filterToolOpen = Var(false)

  val filterTool = div(
    cls := "file-filter",
    label("# of entries ", width := "30px", margin := "0 15 0 10"),
    form(thresholdInput, onSubmit.preventDefault --> { _ ⇒ filterSubmit }),
    label("name ", width := "30px", margin := "0 15 0 10"),
    form(nameInput, onSubmit.preventDefault --> { _ ⇒ filterSubmit })
  )

  def unselectToolAndRefreshTree: Unit = {
    unselectTool
    treeNodePanel.invalidCacheAndDraw
  }

  def selectTool(tool: SelectedTool) = selectedTool.set(Some(tool))

  def unselectTool = {
    clearMessage
    manager.clearSelection
   // newNodeInput.ref.value = ""
    treeNodePanel.treeWarning.set(true)
    treeNodePanel.turnSelectionTo(false)
    selectedTool.set(None)
    treeNodePanel.drawTree
  }

  val deleteButton = button("Delete", btn_danger, onClick --> { _ ⇒ {
    CoreUtils.trashNodes(manager.selected.now) {
      () ⇒
        unselectToolAndRefreshTree
    }
  }
  })

  val copyButton = button("Copy", btn_secondary, onClick --> { _ ⇒ {
    manager.setSelectedAsCopied
    unselectTool
    treeNodePanel.drawTree
  }
  })

  val pluginButton =
    button(
      "Plug",
      btn_secondary,
      onClick --> { _ ⇒
        val directoryName = s"uploadPlugin${java.util.UUID.randomUUID().toString}"
        Post()[Api].copyToPluginUploadDir(directoryName, manager.selected.now).call().foreach { _ ⇒
          import scala.concurrent.duration._
          val names = manager.selected.now.map(_.name)
          Post(timeout = 5 minutes)[Api].addUploadedPlugins(directoryName, names).call().foreach {
            errs ⇒
              if (errs.isEmpty) pluginPanel.pluginDialog.show
              else panels.alertPanel.detail("Plugin import failed", ErrorData.stackTrace(errs.head), transform = RelativeCenterPosition, zone = FileZone)
          }
          unselectToolAndRefreshTree
        }
      }
    )

  //Filter
  implicit def stringToIntOption(s: String): Option[Int] = Try(s.toInt).toOption

  def updateFilter(newFilter: FileFilter) = {
    fileFilter.set(newFilter)
  }

  def switchAlphaSorting = {
    updateFilter(fileFilter.now.switchTo(AlphaSorting()))
    treeNodePanel.invalidCacheAndDraw
  }

  def switchTimeSorting = {
    updateFilter(fileFilter.now.switchTo(TimeSorting()))
    treeNodePanel.invalidCacheAndDraw
  }

  def switchSizeSorting = {
    updateFilter(fileFilter.now.switchTo(SizeSorting()))
    treeNodePanel.invalidCacheAndDraw
  }

  val sortingGroup = {
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
            case Name ⇒ switchAlphaSorting
            case Size ⇒ switchSizeSorting
            case Time ⇒ switchTimeSorting
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
        ),
        div(OMTags.glyph_filter,
          cls <-- fileFilter.signal.map { ff =>
            "filtering-files-item" + {
              if (ff.threshold != Some(1000) || ff.nameFilter != "") "-selected"
              else ""
            }
          },
          onClick --> { _ ⇒ filterToolOpen.update(!_) }),
      ),
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

  def getIfSelected(butt: HtmlElement) = manager.selected.now.map {
    m ⇒
      if (m.isEmpty) div() else butt
  }

  lazy val element = {

    div(
      cls := "file-content",
      div(
        centerElement,
       // buildAndSelectSpan(FileCreationTool, "File or folder creation"),
        buildAndSelectSpan(CopyTool, "Copy selected files"),
        buildAndSelectSpan(TrashTool, "Delete selected files"),
        buildAndSelectSpan(PluginTool, "Detect plugins that can be enabled in this folder"),
        div(
          centerElement,
          buildSpan(RefreshTool, "Refresh the current folder", () ⇒ {
            treeNodePanel.invalidCacheAndDraw
          })),
       // upButton.tooltip("Upload a file")
      ),
      child <-- message.signal.combineWith(selectedTool.signal).map {
        case (msg, sT) ⇒
          div(
            centerFileToolBar,
            msg,
            sT match {
             // case Some(FileCreationTool) ⇒ createFileTool
              case Some(TrashTool) ⇒ getIfSelected(deleteButton)
              case Some(PluginTool) ⇒ getIfSelected(pluginButton)
              case Some(CopyTool) ⇒
                manager.emptyCopied
                getIfSelected(copyButton)
              case _ ⇒ div()
            },
          )
      }
    )
  }

}
