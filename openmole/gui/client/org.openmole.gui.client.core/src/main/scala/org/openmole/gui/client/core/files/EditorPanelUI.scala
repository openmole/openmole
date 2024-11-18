package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.FileExtension.*

import scala.scalajs.js
import scaladget.ace.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.scalajs.js.JSConverters.*
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.tool.OMTags
import org.scalajs.dom.raw.Event
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.{ClickPopup, HoverPopup, Manual, PopupPosition}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import org.openmole.gui.client.tool.Component
import org.openmole.gui.shared.data
import org.scalajs.dom.MouseEvent
import scaladget.bootstrapnative.Tools.MyPopoverBuilder

import scala.scalajs.js.timers.*
import scala.scalajs.js.annotation.JSImport

/*
 * Copyright (C) 07/04/15 // mathieu.leclaire@openmole.org
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

object EditorPanelUI:

  def apply(
    fileType: FileContentType,
    initCode: String,
    initHash: String)(using plugins: GUIPlugins) =
    val editor = new EditorPanelUI(fileType)
    editor.setCode(initCode, initHash)
    editor


  def highlightedFile(fileContentType: FileContentType)(using plugins: GUIPlugins): Option[HighlightedFile] =
    fileContentType match
      case FileContentType.OpenMOLEScript ⇒ Some(HighlightedFile("openmole"))
      case FileContentType.Scala ⇒ Some(HighlightedFile("scala"))
      case FileContentType.Shell ⇒ Some(HighlightedFile("sh"))
      case FileContentType.CSV ⇒ Some(HighlightedFile("csv"))
      case FileContentType.Python => Some(HighlightedFile("python"))
      case ReadableFileType(_, true, Some(hl)) => Some(HighlightedFile(hl))
      case _ ⇒ None

  case class HighlightedFile(highlighter: String)

  @js.native
  @JSImport("ace-builds/src-noconflict/mode-openmole.js", JSImport.Namespace)
  object openmolemode extends js.Object

class EditorPanelUI(fileContentType: FileContentType)(using plugins: GUIPlugins):

  val modified = Var(false)

  val edDiv = div(idAttr := "editor", fontFamily := "monospace")
  val editor = {
    val ed = ace.edit(edDiv.ref)
    val session = ed.getSession()

    js.Dynamic.global.ace.config.set("basePath", "js")
    js.Dynamic.global.ace.config.set("modePath", "js")
    js.Dynamic.global.ace.config.set("themePath", "js")

    scalamode
    pythonmode

    //EditorPanelUI.openmolemode

    githubtheme
    extLanguageTools

    ed.setTheme("ace/theme/github")

    EditorPanelUI.highlightedFile(fileContentType).foreach { h ⇒
      session.setMode("ace/mode/" + h.highlighter)
    }

    ed.renderer.setShowGutter(true)
    ed.setShowPrintMargin(true)
    ed.setAutoScrollEditorIntoView(false)

    ed.setOptions(
      js.Dynamic.literal(
        "enableBasicAutocompletion" -> true,
//        "enableSnippets" -> true,
        "enableLiveAutocompletion" -> true
      )
    )

    //ed.onDocumentChange(() => {println("modif");modified.set(true)})
    ed
  }

  var contentHash = ""

  def onSaved(hash: String) = contentHash = hash

  lazy val lineHeight = Var(17)

  val lineHeightObserver = Observer[Int] { (i: Int) ⇒
    editor.container.style.lineHeight = s"${i}px"
    editor.container.style.fontSize = s"${i - 3}px"
    editor.renderer.updateFontSize()
  }

  val errors: Var[Option[ErrorData]] = Var(None)
  val errorMessage: Var[Option[String]] = Var(None)
  val errorAreaSize: Var[String] = Var("100px")

  def unsetErrors =
    errors.set(None)
    errorMessage.set(None)

//  def setErrorMessage(message: String) =
//    errorMessage.set(Some(message))
//    errorMessageOpen.set(true)

  def errorMessage(e: ScriptError) =
    s"${e.position.map(p => s"${p.line}: ").getOrElse("")}${e.message}"

  def updateErrorMessage =
    errors.now().foreach:
      case error: CompilationErrorData =>
        val selected =
          error.errors.find: e =>
            e.position.map(_.line - 1).contains(editor.selection.getCursor().row.toInt)
        selected match
          case Some(e) =>
            errorMessage.set(Some(errorMessage(e)))
          case _ =>
      case _ =>


  def updateFont(lHeight: Int) =  lineHeight.set(lHeight)

  val errorView =
    div(
      children <--
        errorMessage.signal.map: em =>
            em.toSeq.map: message =>
              div( display.flex,
                textArea(
                  flexRow,
                  message,
                  cls := "scriptError",
                )
            )
    )  

  val view =
    div(
      edDiv.amend(
          lineHeight --> lineHeightObserver,
          fileContentType match
            case FileContentType.OpenMOLEScript ⇒
              errors -->
                Observer[Option[ErrorData]]:
                  case Some(errors: CompilationErrorData) =>
                    editor.getSession().clearBreakpoints()
                    errors.errors.foreach: e ⇒
                      e.position.foreach: p =>
                        editor.getSession().setBreakpoint(p.line - 1)
                    errors.errors.sortBy(_.position.map(_.line).getOrElse(-1)).headOption.foreach: e =>
                      errorMessage.set(Some(errorMessage(e)))
                  case Some(e: MessageErrorData) =>
                    errorMessage.set(Some(e.message))
                  case None =>
                    editor.getSession().clearBreakpoints()
                  case _ =>
            case _ => emptyMod,
          onClick --> { _ => updateErrorMessage }
        )
    )

  def aceDoc = editor.getSession().getDocument()

  def code =
    editor.synchronized {
      (editor.getSession().getValue(), contentHash)
    }

  def setCode(content: String, hash: String) = editor.synchronized {
    contentHash = hash
    editor.getSession().setValue(content)
    modified.set(false)
    editor.getSession().on("change", _ => modified.set(true))
  }

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)
  def hasBeenModified = modified.now()
