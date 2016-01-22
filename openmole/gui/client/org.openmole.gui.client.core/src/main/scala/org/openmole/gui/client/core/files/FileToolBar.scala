package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.ext.data.{ Standby, ProcessState, UploadProject }
import org.openmole.gui.misc.js.{ Select, OMTags }
import org.scalajs.dom.html.Input
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
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

}

import FileToolBar._

class FileToolBar {

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Standby())

  def click(tool: SelectedTool)(action: ⇒ Unit) = {
    action
    selectedTool() = Some(tool)
  }

  def rxClass(sTool: SelectedTool) = Rx {
    "glyphicon " + sTool.glyph + " glyphmenu " + selectedTool().filter(_ == sTool).map { _ ⇒ "selectedTool" }.getOrElse("")
  }

  def buildSpan(selectedTool: SelectedTool, action: ⇒ Unit) = OMTags.glyphSpan(rxClass(selectedTool))(
    click(selectedTool) {
      action
    }
  )

  private def refresh = CoreUtils.refreshCurrentDirectory()

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
    FileManager.upload(fileInput, manager.current.safePath(), (p: ProcessState) ⇒ transferring() = p, UploadProject())
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
            case dt: DirNodeType  ⇒ CoreUtils.addDirectory(currentDirNode, newFile)
            case ft: FileNodeType ⇒ CoreUtils.addFile(currentDirNode, newFile)
          }
        }
      }
      false
    })
  )

  val fileToolDiv = bs.div("fileToolPosition")(
    Rx {
      selectedTool() match {
        case Some(FileCreationTool) ⇒ createFileTool
        case _                      ⇒ tags.div()
      }
    },
    transferring.withWaiter { _ ⇒
      CoreUtils.refreshCurrentDirectory()
      tags.div()
    }
  )

  val div = bs.div("centerFileTool")(
    glyphSpan(glyph_refresh + " glyphmenu", () ⇒ CoreUtils.refreshCurrentDirectory()),
    upButton,
    buildSpan(PluginTool, println("plug")),
    buildSpan(TrashTool, println("trash")),
    buildSpan(CopyTool, println("topy")),
    buildSpan(FileCreationTool, println("plus")),
    buildSpan(FilterTool, println("filter")),
    fileToolDiv
  )
}
