package org.openmole.gui.client.core.files

import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.FileExtension.*

import scala.scalajs.js
import scaladget.ace.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*

import scala.scalajs.js.JSConverters.*
import org.openmole.gui.client.ext.*
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

object EditorPanelUI {

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
}

class EditorPanelUI(fileContentType: FileContentType)(using plugins: GUIPlugins) {

  val modified = Var(false)

  val edDiv = div(idAttr := "editor", fontFamily := "monospace")
  val editor = {
    val ed = ace.edit(edDiv.ref)
    val session = ed.getSession()

    js.Dynamic.global.ace.config.set("basePath", "js")
    js.Dynamic.global.ace.config.set("modePath", "js")
    js.Dynamic.global.ace.config.set("themePath", "js")

    scalamode

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


  val errors = Var(EditorErrors())
  val errorMessage = Var("")
  val errorMessageOpen = Var(false)
  val errorsWithLocation: Var[Seq[ErrorWithLocation]] = Var(Seq())

  def setErrorMessage = {
    errorsWithLocation.now().find { e =>
      e.line.map(_ - 1) == Some(editor.selection.getCursor().row.toInt)
    } match {
      case Some(e) =>
        val message = s"${e.line.getOrElse("")}: ${e.stackTrace}"
        if (errorMessage.now() == message) {
          errorMessageOpen.update(!_)
        }
        else {
          errorMessage.set(message)
          errorMessageOpen.set(true)
        }
      case _ => errorMessageOpen.set(false)
    }
  }

  val omsErrorObserver = Observer[EditorErrors] { (ee: EditorErrors) =>
    val ewls = ee.errorsInEditor.flatMap { i ⇒
      ee.errorsFromCompiler.find(_.errorWithLocation.line == Some(i)).map { e ⇒
        e.errorWithLocation.line.map { l ⇒ editor.getSession().setBreakpoint(l - 1) }
        e.errorWithLocation
      }
    }

    errorsWithLocation.set(ewls)
  }

  def updateFont(lHeight: Int) =  lineHeight.set(lHeight)

  val view =
    div(
      errorMessageOpen.signal.expand(div(
        flexRow,
        child.text <-- errorMessage.signal,
        backgroundColor := "#d35f5f", color := "white", height := "100", padding := "10", fontFamily := "gi")),
      edDiv.amend(
        lineHeight --> lineHeightObserver,
        fileContentType match {
          case FileContentType.OpenMOLEScript ⇒ errors --> omsErrorObserver
          case _ => emptyMod
        },
        onClick --> { _ => setErrorMessage }
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
}

