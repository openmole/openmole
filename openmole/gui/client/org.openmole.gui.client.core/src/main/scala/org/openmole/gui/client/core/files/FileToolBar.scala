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
import org.scalajs.dom.raw.{ HTMLButtonElement, HTMLElement, HTMLInputElement, HTMLSpanElement }
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
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

  object FilterTool extends SelectedTool {
    val glyph = glyph_filter
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
  val transferring: Var[ProcessState] = Var(Processed())
  val fileFilter = Var(FileFilter.defaultFilter)
  val message = Var[Div](div())
  val fileNumberThreshold = 1000

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  def clearMessage = message.set(div())

  def resetFilter = {
    selectedTool.now match {
      case Some(FilterTool) ⇒ manager.invalidCurrentCache
      case _                ⇒
    }
    nameInput.ref.value = ""
    thresholdInput.ref.value = fileNumberThreshold.toString
    resetFilterTools
  }

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
      if (isSelectedTool) selectedTool.now match {
        case Some(FilterTool) ⇒ unselectToolAndRefreshTree
        case _                ⇒ unselectTool
      }
      else {
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
      }
      // todo(isSelectedTool)
    })

  def fInputMultiple(todo: Input ⇒ Unit) =
    inputTag().amend(cls := "upload", `type` := "file", multiple := true,
      inContext { thisNode ⇒
        onChange --> { _ ⇒
          todo(thisNode)
        }
      }
    )

  //Upload tool
  def upbtn(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, glyph_upload, "fileUpload glyphmenu",
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: Input) ⇒ {
    FileManager.upload(fileInput, manager.current.now, (p: ProcessState) ⇒ transferring.set(p), UploadProject(), () ⇒ treeNodePanel.invalidCacheAndDraw)
  })

  // New file tool
  val newNodeInput = inputTag().amend(
    placeholder := "File name",
    width := "130px",
    left := "-2px",
    onMountFocus
  )

  lazy val addRootDirButton = {

    val folder = ToggleState("Folder", btn_primary_string, () ⇒ {})
    val file = ToggleState("File", btn_secondary_string, () ⇒ {})

    toggle(folder, true, file, () ⇒ {})
  }

  // Filter tool
  val thresholdTag = "threshold"
  val nameTag = "names"
  val thresholdChanged = Var(false)

  val thresholdInput = inputTag(fileNumberThreshold.toString).amend(
    idAttr := thresholdTag,
    width := "60px",
    onMountFocus,
    cls := "form-control",
    cls.toggle("colorTransition") <-- thresholdChanged.signal
  )

  val nameInput = inputTag("").amend(
    idAttr := nameTag,
    width := "70px",
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

  def filterSubmit: () ⇒ Boolean = () ⇒ {
    resetFilterTools
    treeNodePanel.invalidCacheAndDraw
    false
  }

  val filterTool = div(
    centerElement,
    span(tdStyle, label("# of entries ", labelStyle)),
    span(tdStyle, form(thresholdInput, onSubmit --> { _ ⇒ filterSubmit })),
    span(tdStyle, label("name ", forId := nameTag, labelStyle)),
    span(tdStyle, form(nameInput, onSubmit --> { _ ⇒ filterSubmit }))
  )

  def createNewNode = {
    val newFile = newNodeInput.ref.value
    val currentDirNode = manager.current
    addRootDirButton.toggled.now match {
      case true  ⇒ CoreUtils.addDirectory(currentDirNode.now, newFile, () ⇒ unselectToolAndRefreshTree)
      case false ⇒ CoreUtils.addFile(currentDirNode.now, newFile, () ⇒ unselectToolAndRefreshTree)
    }
  }

  val createFileTool = div(
    addRootDirButton.element,
    form(newNodeInput, onSubmit --> { _ ⇒
      createNewNode
      false
    })
  )

  def unselectToolAndRefreshTree: Unit = {
    unselectTool
    treeNodePanel.invalidCacheAndDraw
  }

  def selectTool(tool: SelectedTool) = selectedTool.set(Some(tool))

  def unselectTool = {
    clearMessage
    resetFilter
    manager.clearSelection
    newNodeInput.ref.value = ""
    treeNodePanel.treeWarning.set(true)
    treeNodePanel.turnSelectionTo(false)
    selectedTool.set(None)
    treeNodePanel.drawTree
  }

  val deleteButton = button("Delete", btn_danger, onClick --> { _ ⇒
    {
      CoreUtils.trashNodes(manager.selected.now) {
        () ⇒
          unselectToolAndRefreshTree
      }
    }
  })

  val copyButton = button("Copy", btn_secondary, onClick --> { _ ⇒
    {
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
    val topTriangle = Seq(glyph_triangle_top, (fontSize := "10"))
    val bottomTriangle = Seq(glyph_triangle_bottom, fontSize := "10")
    //    exclusiveButtonGroup(omsheet.sortingBar, cls := ("sortingTool", "selectedSortingTool"))(
    //      ExclusiveButton.twoGlyphSpan(
    //        topTriangle,
    //        bottomTriangle,
    //        () ⇒ switchAlphaSorting,
    //        () ⇒ switchAlphaSorting,
    //        preString = "Aa",
    //        cls := "sortingTool"
    //      ),
    //      ExclusiveButton.twoGlyphButtonStates(
    //        topTriangle,
    //        bottomTriangle,
    //        () ⇒ switchTimeSorting,
    //        () ⇒ switchTimeSorting,
    //        preGlyph = Seq(twoGlyphButton, glyph_time)
    //      ),
    //      ExclusiveButton.twoGlyphButtonStates(
    //        topTriangle,
    //        bottomTriangle,
    //        () ⇒ switchSizeSorting,
    //        () ⇒ switchSizeSorting,
    //        preGlyph = Seq(twoGlyphButton, OMTags.glyph_data, paddingTop := 10, fontSize := 12)
    //      )
    //  )
    div()
  }

  def getIfSelected(butt: HtmlElement) = manager.selected.now.map { m ⇒
    if (m.isEmpty) div() else butt
  }

  lazy val element = {

    div(paddingBottom := "10", paddingTop := "50",
      div(
        centerElement,
        buildAndSelectSpan(FilterTool, "Filter files by number of entries or by names"),
        buildAndSelectSpan(FileCreationTool, "File or folder creation"),
        buildAndSelectSpan(CopyTool, "Copy selected files"),
        buildAndSelectSpan(TrashTool, "Delete selected files"),
        buildAndSelectSpan(PluginTool, "Detect plugins that can be enabled in this folder"),
        div(
          centerElement,
          buildSpan(RefreshTool, "Refresh the current folder", () ⇒ {
            treeNodePanel.invalidCacheAndDraw
          })),
        upButton.tooltip("Upload a file")
      ),
      child <-- message.signal.combineWith(selectedTool.signal).map {
        case (msg, sT) ⇒
          div(
            centerFileToolBar,
            msg,
            sT match {
              case Some(FilterTool) ⇒
                treeNodePanel.treeWarning.set(false)
                filterTool
              case Some(FileCreationTool) ⇒ createFileTool
              case Some(TrashTool)        ⇒ getIfSelected(deleteButton)
              case Some(PluginTool)       ⇒ getIfSelected(pluginButton)
              case Some(CopyTool) ⇒
                manager.emptyCopied
                getIfSelected(copyButton)
              case _ ⇒ div()
            },
            transferring.withTransferWaiter {
              _ ⇒
                div()
            }
          )
      }
    )
  }

}
