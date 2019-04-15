package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import org.scalajs.dom.html.Input

import scala.util.Try
import scalatags.JsDom.tags
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import autowire._
import org.openmole.gui.ext.tool.client._
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.scalajs.dom.raw.{ HTMLButtonElement, HTMLElement, HTMLInputElement, HTMLSpanElement }
import rx._
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.panels._
import org.openmole.gui.client.tool._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.client.core._
import org.openmole.gui.ext.tool.client.FileManager

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
    def glyph: Glyphicon
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
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val fileFilter = Var(FileFilter.defaultFilter)
  val message = Var(tags.div)
  val fileNumberThreshold = 1000

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  def clearMessage = message() = tags.div

  def resetFilter = {
    selectedTool.now match {
      case Some(FilterTool) ⇒ manager.invalidCurrentCache
      case _                ⇒
    }
    nameInput.value = ""
    thresholdInput.value = fileNumberThreshold.toString
    resetFilterTools
  }

  def buildSpan(tool: SelectedTool, legend: String, todo: () ⇒ Unit, modifierSeq: ModifierSeq = emptyMod): HTMLElement = {
    span(
      tool.glyph +++ pointer +++ modifierSeq +++ "glyphmenu",
      onclick := { () ⇒
        {
          todo()
        }
      }
    ).tooltip(legend)
  }

  def buildAndSelectSpan(tool: SelectedTool, legend: String, todo: Boolean ⇒ Unit = (Boolean) ⇒ {}): HTMLElement = buildSpan(tool, legend, { () ⇒
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
              message() = tags.div(omsheet.color("white"), "No plugin could be found in this folder.")
            else {
              clearMessage
              treeNodePanel.turnSelectionTo(true)
            })
        case _ ⇒
      }
      selectedTool() = Some(tool)
    }
    // todo(isSelectedTool)
  })

  def fInputMultiple(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = inputTag()(ms("upload"), `type` := "file", multiple := "")(onchange := { () ⇒
      todo(input)
    }).render
    input
  }

  //Upload tool
  def upbtn(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] =
    span(aria.hidden := "true", glyph_upload +++ "fileUpload glyphmenu")(
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: HTMLInputElement) ⇒ {
    FileManager.upload(fileInput, manager.current.now, (p: ProcessState) ⇒ transferring() = p, UploadProject(), () ⇒ treeNodePanel.invalidCacheAndDraw)
  })

  // New file tool
  val newNodeInput: Input = inputTag()(
    placeholder := "File name",
    width := "130px",
    left := "-2px",
    autofocus
  ).render

  lazy val addRootDirButton: Options[TreeNodeType] = {
    val file = TreeNodeType.file
    val folder = TreeNodeType.folder

    val contents = Seq(file, folder)
    contents.options(
      0,
      btn_default +++ borderRightFlat,
      (e: TreeNodeType) ⇒ e.name, onclickExtra = () ⇒ {
        addRootDirButton.content.now.foreach { c ⇒
          newNodeInput.placeholder = c.name + " name"
        }
      },
      decorations = Map(file → glyph_file, folder → glyph_folder_close)
    )
  }

  // Filter tool
  val thresholdTag = "threshold"
  val nameTag = "names"
  val thresholdChanged = Var(false)

  val thresholdInput = inputTag(fileNumberThreshold.toString)(
    id := thresholdTag,
    width := "60px",
    autofocus,
    `class` := Rx {
      "form-control " + {
        if (thresholdChanged()) "colorTransition"
        else ""
      }
    }
  ).render

  val nameInput = inputTag("")(
    id := nameTag,
    width := "70px",
    autofocus
  ).render

  def updateFilter: Unit = updateFilter(fileFilter.now.copy(threshold = thresholdInput.value, nameFilter = nameInput.value))

  def resetFilterTools = {
    if (thresholdInput.value > "1000") {
      thresholdInput.value = "1000"
      thresholdChanged() = true
    }
    else thresholdChanged() = false
    updateFilter(fileFilter.now.copy(threshold = thresholdInput.value, nameFilter = nameInput.value))
  }

  def filterSubmit: () ⇒ Boolean = () ⇒ {
    resetFilterTools
    treeNodePanel.invalidCacheAndDraw
    false
  }

  val filterTool = tags.div(centerElement)(
    tags.span(tdStyle)(label("# of entries ")(labelStyle)),
    tags.span(tdStyle)(form(thresholdInput, onsubmit := filterSubmit)),
    tags.span(tdStyle)(label("name ")(`for` := nameTag, labelStyle)),
    tags.span(tdStyle)(form(nameInput, onsubmit := filterSubmit))
  )

  def createNewNode = {
    val newFile = newNodeInput.value
    val currentDirNode = manager.current
    addRootDirButton.get match {
      case Some(dt: DirNodeType)  ⇒ CoreUtils.addDirectory(currentDirNode.now, newFile, () ⇒ unselectToolAndRefreshTree)
      case Some(ft: FileNodeType) ⇒ CoreUtils.addFile(currentDirNode.now, newFile, () ⇒ unselectToolAndRefreshTree)
      case _                      ⇒
    }
  }

  val createFileTool = inputGroup()(
    inputGroupButton(addRootDirButton.selector),
    form(newNodeInput, onsubmit := {
      () ⇒
        createNewNode
        false
    })
  ).render

  def unselectToolAndRefreshTree: Unit = {
    unselectTool
    treeNodePanel.invalidCacheAndDraw
  }

  def selectTool(tool: SelectedTool) = selectedTool() = Some(tool)

  def unselectTool = {
    clearMessage
    resetFilter
    manager.clearSelection
    newNodeInput.value = ""
    treeNodePanel.treeWarning() = true
    treeNodePanel.turnSelectionTo(false)
    selectedTool() = None
    treeNodePanel.drawTree
  }

  val deleteButton = button("Delete", btn_danger, onclick := { () ⇒
    {
      CoreUtils.trashNodes(manager.selected.now) {
        () ⇒
          unselectToolAndRefreshTree
      }
    }
  })

  val copyButton = button("Copy", btn_default, onclick := { () ⇒
    {
      manager.setSelectedAsCopied
      unselectTool
      treeNodePanel.drawTree
    }
  })

  val pluginButton = button("Plug", btn_default, onclick := { () ⇒
    {
      post()[Api].copyToPluginUploadDir(manager.selected.now).call().foreach {
        c ⇒
          post()[Api].addUploadedPlugins(manager.selected.now.map {
            _.name
          }).call().foreach {
            errs ⇒
              if (errs.isEmpty) {
                pluginPanel.dialog.show
              }
              else AlertPanel.detail("Plugin import failed", ErrorData.stackTrace(errs.head), transform = RelativeCenterPosition, zone = FileZone)
          }
          unselectToolAndRefreshTree
      }
    }
  })

  //Filter
  implicit def stringToIntOption(s: String): Option[Int] = Try(s.toInt).toOption

  def updateFilter(newFilter: FileFilter) = {
    fileFilter() = newFilter
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
    val topTriangle = glyph_triangle_top +++ (fontSize := 10)
    val bottomTriangle = glyph_triangle_bottom +++ (fontSize := 10)
    exclusiveButtonGroup(omsheet.sortingBar, ms("sortingTool"), ms("selectedSortingTool"))(
      ExclusiveButton.twoGlyphSpan(
        topTriangle,
        bottomTriangle,
        () ⇒ switchAlphaSorting,
        () ⇒ switchAlphaSorting,
        preString = "Aa",
        ms("sortingTool")
      ),
      ExclusiveButton.twoGlyphButtonStates(
        topTriangle,
        bottomTriangle,
        () ⇒ switchTimeSorting,
        () ⇒ switchTimeSorting,
        preGlyph = twoGlyphButton +++ glyph_time
      ),
      ExclusiveButton.twoGlyphButtonStates(
        topTriangle,
        bottomTriangle,
        () ⇒ switchSizeSorting,
        () ⇒ switchSizeSorting,
        preGlyph = twoGlyphButton +++ OMTags.glyph_data +++ Seq(paddingTop := 10, fontSize := 12)
      )
    )
  }

  def getIfSelected(butt: TypedTag[HTMLButtonElement]) = manager.selected.map {
    m ⇒
      if (m.isEmpty) tags.div else butt
  }

  lazy val div = {

    tags.div(paddingBottom := 10, paddingTop := 50)(
      tags.div(centerElement)(
        buildAndSelectSpan(FilterTool, "Filter files by number of entries or by names"),
        buildAndSelectSpan(FileCreationTool, "File or folder creation"),
        buildAndSelectSpan(CopyTool, "Copy selected files"),
        buildAndSelectSpan(TrashTool, "Delete selected files"),
        buildAndSelectSpan(PluginTool, "Detect plugins that can be enabled in this folder"),
        tags.div(centerElement)(
          buildSpan(RefreshTool, "Refresh the current folder", () ⇒ {
            treeNodePanel.invalidCacheAndDraw
          })),
        upButton.tooltip("Upload a file")
      ),
      Rx {
        val msg = message()
        val sT = selectedTool()
        tags.div(centerFileToolBar)(
          msg,
          sT match {
            case Some(FilterTool) ⇒
              treeNodePanel.treeWarning() = false
              filterTool
            case Some(FileCreationTool) ⇒ createFileTool
            case Some(TrashTool)        ⇒ getIfSelected(deleteButton)
            case Some(PluginTool)       ⇒ getIfSelected(pluginButton)
            case Some(CopyTool) ⇒
              manager.emptyCopied
              getIfSelected(copyButton)
            case _ ⇒ tags.div()
          },
          transferring.withTransferWaiter {
            _ ⇒
              tags.div()
          }
        )
      }
    )
  }

}
