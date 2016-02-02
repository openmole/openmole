package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.{ Select, OMTags }
import org.scalajs.dom.html.Input
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
import org.openmole.gui.client.core.ClientProcessState._

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
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
  }

  object FilterTool extends SelectedTool {
    val glyph = OMTags.glyph_filter
  }

  object FileCreationTool extends SelectedTool {
    val glyph = glyph_plus
  }

  object PluginTool extends SelectedTool {
    val glyph = OMTags.glyph_plug
  }

  object CopyTool extends SelectedTool {
    val glyph = OMTags.glyph_copy
  }

  object PasteTool extends SelectedTool {
    val glyph = OMTags.glyph_copy
  }

}

import FileToolBar._

class FileToolBar(onrefreshed: () ⇒ Unit) {

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())

  def click(tool: SelectedTool, action: () ⇒ Unit) = doSelection(tool, action)

  def rxClass(sTool: SelectedTool) = Rx {
    "glyphicon " + sTool.glyph + " glyphmenu " + selectedTool().filter(_ == sTool).map { _ ⇒ "selectedTool" }.getOrElse("")
  }

  def buildSpan(selectedTool: SelectedTool, action: () ⇒ Unit = () ⇒ {}) = OMTags.glyphSpan(rxClass(selectedTool))(
    click(selectedTool, action)
  )

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
    FileManager.upload(fileInput, manager.current.safePath(), (p: ProcessState) ⇒ transferring() = p, UploadProject(), onrefreshed)
  })

  // New file tool
  val newNodeInput: Input = bs.input("")(
    placeholder := "File name",
    width := "130px",
    autofocus
  ).render

  val addRootDirButton: Select[TreeNodeType] = {
    val content = Seq((TreeNodeType.file, key(glyph_file)), (TreeNodeType.folder, key(glyph_folder_close)))
    Select("fileOrFolder", content, content.map {
      _._1
    }.headOption, btn_success + "borderRightFlat", () ⇒ {
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
    onrefreshed()
  }

  def unselectTool = selectedTool() = None

  def doSelection(tool: SelectedTool, onactivated: () ⇒ Unit) = selectedTool() match {
    case Some(tool) ⇒ unselectTool
    case _ ⇒
      selectedTool() = Some(tool)
      onactivated()
  }

  val deleteButton = bs.button("Delete", btn_danger, () ⇒ {
    CoreUtils.trashNodes(manager.selected()) { () ⇒
      unselectAndRefreshTree
    }
  })

  val copyButton = bs.button("Copy", btn_default, () ⇒ {
    manager.setSelectedAsCopied
  })

  val pasteButton = bs.button("Paste", btn_success, () ⇒ {
    paste(manager.copied(), manager.current)
  })

  val pluginButton = bs.button("Get plugins", btn_default, () ⇒ {
    unselectAndRefreshTree
  })

  val fileToolDiv = bs.div("toolPosition")(
    Rx {
      selectedTool() match {
        case Some(FileCreationTool) ⇒ createFileTool
        case Some(TrashTool)        ⇒ deleteButton
        case Some(CopyTool)         ⇒ copyButton
        case Some(PasteTool)        ⇒ pasteButton
        case Some(PluginTool)       ⇒ pluginButton
        case _                      ⇒ tags.div()
      }
    },
    transferring.withWaiter { _ ⇒
      tags.div()
    }
  )

  val div = bs.div("centerFileTool")(
    tags.div(
      glyphSpan(glyph_refresh + " glyphmenu", () ⇒ {
        CoreUtils.refreshCurrentDirectory(onrefreshed)
      }),
      upButton,
      buildSpan(PluginTool, () ⇒ setSelection),
      buildSpan(TrashTool, () ⇒ setSelection),
      buildSpan(CopyTool, () ⇒ setSelection),
      buildSpan(FileCreationTool),
      buildSpan(FilterTool, () ⇒ println("filter"))
    ),
    fileToolDiv
  )

  private def setSelection = {
    manager.setSelection
    onrefreshed()
  }

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      manager.noError
      CoreUtils.refreshAndSwitchSelection(onrefreshed)
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
