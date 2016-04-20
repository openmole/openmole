package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.{ Select, OMTags }
import org.openmole.gui.misc.utils.stylesheet
import org.scalajs.dom.html.Input
import scala.util.Try
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.stylesheet._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import sheet._
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
    def glyph: Glyphicon

    def checkMode: Boolean
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
    val checkMode = true
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
    val glyph = glyph_copy
    val checkMode = true
  }

  object PasteTool extends SelectedTool {
    val glyph = glyph_copy
    val checkMode = false
  }

  object RefreshTool extends SelectedTool {
    val glyph = glyph_refresh
    val checkMode = false
  }

}

import FileToolBar._

class FileToolBar(refreshAndRedraw: () ⇒ Unit) {

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val fileFilter = Var(FileFilter.defaultFilter)

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  implicit def toolToSetSelection(t: SelectedTool): () ⇒ Unit = () ⇒ manager.setSelection(t)

  def hasFilter = fileFilter() != FileFilter.defaultFilter

  def resetFilter = {
    fileFilter() = FileFilter.defaultFilter
    byNameInput.value = fileFilter().nameFilter
    thresholdInput.value = fileFilter().threshold
  }

  def buildSpan(tool: SelectedTool, todo: () ⇒ Unit): Rx[TypedTag[HTMLSpanElement]] = Rx {
    span(
      tool.glyph +++ pointer +++ selectedTool().filter(_ == tool).map { _ ⇒ stylesheet.selectedTool }.getOrElse(emptyMod) +++ "glyphmenu",
      onclick := todo
    )
  }

  def buildSpan(tool: SelectedTool): Rx[TypedTag[HTMLSpanElement]] = buildSpan(tool, { () ⇒
    if (selectedTool() == Some(tool)) unselectTool
    else selectedTool() = Some(tool)
    val a: () ⇒ Unit = tool
    a()
  })

  def fInputMultiple(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = bs.input()(ms("upload"), `type` := "file", multiple := "")(onchange := { () ⇒
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
    FileManager.upload(fileInput, manager.current.safePath(), (p: ProcessState) ⇒ transferring() = p, UploadProject(), refreshAndRedraw)
  })

  // New file tool
  val newNodeInput: Input = bs.input()(
    placeholder := "File name",
    width := "130px",
    left := "-2px",
    autofocus
  ).render

  val addRootDirButton: Select[TreeNodeType] = {
    val content = Seq((TreeNodeType.file, glyph_file +++ sheet.paddingLeft(3)), (TreeNodeType.folder, glyph_folder_close +++ sheet.paddingLeft(3)))
    Select( /*"fileOrFolder", */ content, content.map {
      _._1
    }.headOption, btn_default +++ borderRightFlat, () ⇒ {
      addRootDirButton.content().map { c ⇒ newNodeInput.placeholder = c.name + " name" }
    })
  }

  val createFileTool = bs.inputGroup(navbar_left)(
    bs.inputGroupButton(addRootDirButton.selector),
    form(newNodeInput, onsubmit := { () ⇒
      {
        val newFile = newNodeInput.value
        val currentDirNode = manager.current
        addRootDirButton.content().map {
          _ match {
            case dt: DirNodeType  ⇒ CoreUtils.addDirectory(currentDirNode, newFile, () ⇒ unselectAndRefreshTree)
            case ft: FileNodeType ⇒ CoreUtils.addFile(currentDirNode, newFile, () ⇒ unselectAndRefreshTree)
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

  def updateFilter(newFilter: FileFilter) = {
    fileFilter() = newFilter
    refreshAndRedraw()
  }

  lazy val byNameInput = bs.input(fileFilter().nameFilter)(smallInput, placeholder := "Name").render

  lazy val byNameForm = form(filterElement)(
    byNameInput,
    onsubmit := { () ⇒
      updateFilter(fileFilter().copy(nameFilter = byNameInput.value))
      false
    }
  )

  lazy val thresholdInput = bs.input(fileFilter().threshold)(smallInput, placeholder := "Size").render

  lazy val thresholdForm = form(filterElement)(
    thresholdInput,
    onsubmit := { () ⇒
      updateFilter(fileFilter().copy(threshold = thresholdInput.value))
      false
    }
  )

  def switchAlphaSorting = updateFilter(fileFilter().copy(fileSorting = AlphaSorting).switch)
  def switchTimeSorting = updateFilter(fileFilter().copy(fileSorting = TimeSorting).switch)
  def switchSizeSorting = updateFilter(fileFilter().copy(fileSorting = SizeSorting).switch)

  val sortingGroup = bs.exclusiveButtonGroup(stylesheet.sortingBar, ms("sortingTool"), ms("selectedSortingTool"))(
    ExclusiveButton.twoGlyphStates(
      glyph_triangle_top,
      glyph_triangle_bottom,
      () ⇒ switchAlphaSorting,
      () ⇒ switchAlphaSorting,
      preString = "Aa",
      twoGlyphButton
    ),
    ExclusiveButton.twoGlyphStates(
      glyph_triangle_top,
      glyph_triangle_bottom,
      () ⇒ switchTimeSorting,
      () ⇒ switchTimeSorting,
      preGlyph = twoGlyphButton +++ glyph_time
    ),
    ExclusiveButton.twoGlyphStates(
      glyph_triangle_top,
      glyph_triangle_bottom,
      () ⇒ switchSizeSorting,
      () ⇒ switchSizeSorting,
      preString = "Kb",
      twoGlyphButton
    )
  )

  val fileToolDiv = Rx {
    tags.div(centerElement +++ sheet.marginTop(5) +++ sheet.marginBottom(15))(
      selectedTool() match {
        case Some(FileCreationTool) ⇒ createFileTool
        case Some(TrashTool)        ⇒ deleteButton
        case Some(CopyTool)         ⇒ copyButton
        case Some(PasteTool)        ⇒ pasteButton
        case Some(PluginTool)       ⇒ pluginButton
        case _                      ⇒ tags.div()
      },
      transferring.withTransferWaiter { _ ⇒
        tags.div()
      }
    )
  }

  val div = tags.div(
    tags.div(centerElement)(
      buildSpan(RefreshTool, refreshAndRedraw),
      upButton,
      buildSpan(PluginTool),
      buildSpan(TrashTool),
      buildSpan(CopyTool),
      buildSpan(FileCreationTool)
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
