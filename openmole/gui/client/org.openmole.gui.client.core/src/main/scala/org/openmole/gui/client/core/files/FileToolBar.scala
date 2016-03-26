package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.OMTags.ExclusiveButton
import org.openmole.gui.misc.js.{ Select, OMTags }
import org.scalajs.dom.html.Input
import scala.util.Try
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.TreeNode._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import bs._
import org.scalajs.dom.raw.{ HTMLSpanElement, HTMLInputElement }
import rx._
import org.openmole.gui.client.core.Waiter._

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

  sealed trait SelectedTool {
    def glyph: String
    def checkMode: Boolean
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
    val checkMode = true
  }

  object FilterTool extends SelectedTool {
    val glyph = OMTags.glyph_filter
    val checkMode = false
  }

  object FileCreationTool extends SelectedTool {
    val glyph = glyph_plus
    val checkMode = false
  }

  object PluginTool extends SelectedTool {
    val glyph = OMTags.glyph_plug
    val checkMode = false
  }

  object CopyTool extends SelectedTool {
    val glyph = OMTags.glyph_copy
    val checkMode = true
  }

  object PasteTool extends SelectedTool {
    val glyph = OMTags.glyph_copy
    val checkMode = false
  }

}

import FileToolBar._

class FileToolBar(redraw: () ⇒ Unit, refreshAndRedraw: () ⇒ Unit) {

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val fileFilter = Var(FileFilter.defaultFilter)
  val treeSorting = Var(TreeSorting.defaultSorting)

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  implicit def toolToSetSelection(t: SelectedTool): () ⇒ Unit = () ⇒ manager.setSelection(t)

  def hasFilter = fileFilter() != FileFilter.defaultFilter

  def resetFilter = {
    fileFilter() = FileFilter.defaultFilter
    byNameInput.value = fileFilter().nameFilter
    thresholdInput.value = fileFilter().threshold
    firstLastGroup.reset
  }

  def rxClass(sTool: SelectedTool) = Rx {
    "glyphicon " + sTool.glyph + " glyphmenu " + selectedTool().filter(_ == sTool).map { _ ⇒ "selectedTool" }.getOrElse("")
  }

  def buildSpan(tool: SelectedTool) = OMTags.glyphSpan(rxClass(tool)) { () ⇒
    if (selectedTool() == Some(tool)) unselectTool
    else selectedTool() = Some(tool)
    val a: () ⇒ Unit = tool
    a()
  }

  def fInputMultiple(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = tags.input(`class` := "upload", `type` := "file", multiple := "")(onchange := { () ⇒
      todo(input)
    }).render
    input
  }

  //Upload tool
  def upbtn(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] =
    glyph(glyph_upload + " fileUpload glyphmenu")(
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: HTMLInputElement) ⇒ {
    FileManager.upload(fileInput, manager.current.safePath(), (p: ProcessState) ⇒ transferring() = p, UploadProject(), refreshAndRedraw)
  })

  // New file tool
  val newNodeInput: Input = bs.input("")(
    placeholder := "File name",
    width := "130px",
    left := "-2px",
    autofocus
  ).render

  val addRootDirButton: Select[TreeNodeType] = {
    val content = Seq((TreeNodeType.file, key(glyph_file) + "paddingLeft3"), (TreeNodeType.folder, key(glyph_folder_close) + "paddingLeft3"))
    Select("fileOrFolder", content, content.map {
      _._1
    }.headOption, btn_default + "borderRightFlat", () ⇒ {
      addRootDirButton.content().map { c ⇒ newNodeInput.placeholder = c.name + " name" }
    })
  }

  val createFileTool = inputGroup(navbar_left)(
    inputGroupButton(addRootDirButton.selector),
    tags.form(newNodeInput, onsubmit := { () ⇒
      {
        val newFile = newNodeInput.value
        val currentDirNode = manager.current
        addRootDirButton.content().map {
          _ match {
            case dt: DirNodeType  ⇒ CoreUtils.addDirectory(currentDirNode, newFile, fileFilter(), () ⇒ unselectAndRefreshTree)
            case ft: FileNodeType ⇒ CoreUtils.addFile(currentDirNode, newFile, fileFilter(), () ⇒ unselectAndRefreshTree)
          }
        }
      }
      false
    })
  )

  def unselectAndRefreshTree: Unit = {
    unselectTool
    newNodeInput.value = ""
    refreshAndRedraw()
  }

  def unselectTool = selectedTool() = None

  val deleteButton = bs.button("Delete", btn_danger, () ⇒ {
    CoreUtils.trashNodes(manager.selected(), fileFilter()) { () ⇒
      unselectAndRefreshTree
    }
  })

  val copyButton = bs.button("Copy", btn_default, () ⇒ {
    manager.setSelectedAsCopied
    selectedTool() = Some(PasteTool)
  })

  val pasteButton = bs.button("Paste", btn_primary, () ⇒ {
    paste(manager.copied(), manager.current)
  })

  val pluginButton = bs.button("Get plugins", btn_default, () ⇒ {
    unselectAndRefreshTree
  })

  //Filter
  implicit def stringToIntOption(s: String): Option[Int] = Try(thresholdInput.value.toInt).toOption

  def updateFilter(filterChange: () ⇒ FileFilter) = {
    val previous = fileFilter()
    fileFilter() = filterChange()
    if (previous != fileFilter()) refreshAndRedraw()
    else redraw()
  }

  def updateSorting(sortingChange: () ⇒ TreeSorting) = {
    treeSorting() = sortingChange()
    redraw()
  }

  lazy val byNameInput = bs.input(fileFilter().nameFilter, "smallInput")(placeholder := "Name").render

  lazy val byNameForm = bs.form("filterElement")(
    byNameInput,
    onsubmit := { () ⇒
      updateFilter(() ⇒ fileFilter().copy(nameFilter = byNameInput.value))
      false
    }
  )

  lazy val thresholdInput = bs.input(fileFilter().threshold, "smallInput")(placeholder := "Size").render

  lazy val thresholdForm = bs.form("filterElement")(
    thresholdInput,
    onsubmit := { () ⇒
      updateFilter(() ⇒ fileFilter().copy(threshold = thresholdInput.value))
      false
    }
  )

  val firstLastGroup = OMTags.buttonGroupExclusive("filterElement")(
    ExclusiveButton.string("first", () ⇒ updateFilter(() ⇒ fileFilter().copy(firstLast = First))),
    ExclusiveButton.string("last", () ⇒ updateFilter(() ⇒ fileFilter().copy(firstLast = Last)))
  )

  val sortingGroupInFilter = OMTags.buttonGroupExclusive("filterElement")(
    ExclusiveButton.glyph(OMTags.glyph_alph_sorting, () ⇒ updateFilter(() ⇒ fileFilter().copy(fileSorting = AlphaSorting))),
    ExclusiveButton.string("KB", () ⇒ updateFilter(() ⇒ fileFilter().copy(fileSorting = SizeSorting))),
    ExclusiveButton.glyph(OMTags.glyph_time, () ⇒ updateFilter(() ⇒ fileFilter().copy(fileSorting = TimeSorting)))
  )

  val sortingGroup = OMTags.buttonGroupExclusive("sortingBar")(
    ExclusiveButton.glyph(OMTags.glyph_alph_sorting, () ⇒ updateSorting(() ⇒ treeSorting().copy(fileSorting = AlphaSorting))),
    ExclusiveButton.string("KB", () ⇒ updateSorting(() ⇒ treeSorting().copy(fileSorting = SizeSorting))),
    ExclusiveButton.glyph(OMTags.glyph_time, () ⇒ updateSorting(() ⇒ treeSorting().copy(fileSorting = TimeSorting))),
    ExclusiveButton.twoGlyphStates(
      OMTags.glyph_triangle_bottom,
      OMTags.glyph_triangle_top,
      () ⇒ updateSorting(() ⇒ treeSorting().copy(fileOrdering = Ascending)),
      () ⇒ updateSorting(() ⇒ treeSorting().copy(fileOrdering = Descending))
    )
  )

  lazy val filterDiv =
    bs.div("")(
      bs.div("filterLine marginLeft10")(
        bs.div("white filterElement spacer6")("Take"),
        firstLastGroup.div,
        thresholdForm
      ),
      bs.div("filterLine marginLeft-20")(
        bs.div("white filterElement spacer6")("filtered by"),
        sortingGroupInFilter.div,
        bs.div("white filterElement spacer6")("and by"),
        byNameForm
      )
    )

  val fileToolDiv = bs.div("toolPosition")(
    Rx {
      selectedTool() match {
        case Some(FileCreationTool) ⇒ createFileTool
        case Some(TrashTool)        ⇒ deleteButton
        case Some(CopyTool)         ⇒ copyButton
        case Some(PasteTool)        ⇒ pasteButton
        case Some(PluginTool)       ⇒ pluginButton
        case Some(FilterTool)       ⇒ filterDiv
        case _                      ⇒ tags.div()
      }
    },
    transferring.withTransferWaiter { _ ⇒
      tags.div()
    }
  )

  val div = bs.div("centerFileTool")(
    tags.div(
      glyphSpan(glyph_refresh + " glyphmenu", refreshAndRedraw),
      upButton,
      buildSpan(PluginTool),
      buildSpan(TrashTool),
      buildSpan(CopyTool),
      buildSpan(FileCreationTool),
      buildSpan(FilterTool)
    ),
    fileToolDiv
  )

  //inTreanodepanel
  /*private def filter( in: TreeNodeData, fileFilter: FileFilter) = {
    CoreUtils.filter(in, fileFilter).withFutureWaiter { _ ⇒
      tags.div("filtering...")
    }("Searching...", (s: Seq[TreeNodeData]) ⇒ println("success " + s))
  }*/

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      manager.noError
      refreshAndRedraw()
    }

    def onpasted = {
      manager.emptyCopied
      unselectTool
    }

    val same = safePaths.filter { sp ⇒
      sp == to
    }
    if (same.isEmpty) {
      CoreUtils.testExistenceAndCopyProjectFilesTo(safePaths, to).foreach { existing ⇒
        if (existing.isEmpty) {
          refreshWithNoError
          onpasted
        }
        else manager.setFilesInError(
          "Some files already exists, overwrite ?",
          existing,
          () ⇒ CoreUtils.copyProjectFilesTo(safePaths, to).foreach { b ⇒
            refreshWithNoError
            onpasted
          }, () ⇒ {
            refreshWithNoError
            unselectTool
          }
        )
      }
    }
    else manager.setFilesInComment(
      "Paste a folder in itself is not allowed",
      same,
      () ⇒ manager.noError
    )
  }

}
