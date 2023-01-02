package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._

import scala.scalajs.js
import scaladget.ace._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.data.DataUtils._

import scala.scalajs.js.JSConverters._
import org.openmole.gui.ext.client._
import org.scalajs.dom.raw.Event
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.{ClickPopup, HoverPopup, Manual, PopupPosition}
import com.raquo.laminar.api.L._
import com.raquo.laminar.builders.DomEventStreamPropBuilder
import com.raquo.laminar.nodes.ReactiveElement
import org.openmole.gui.client.core.staticPanels
import org.openmole.gui.ext.data
import org.scalajs.dom.MouseEvent
import scaladget.bootstrapnative.Tools.MyPopoverBuilder
import scala.scalajs.js.timers._

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

  def apply(fileType: FileExtension,
            initCode: String,
            initHash: String
            //containerHESetters: HESetters     = emptySetters
           ) = {

    val editor = {
      fileType match {
        case OMS ⇒ new EditorPanelUI(OMS)
        case SCALA ⇒ new EditorPanelUI(SCALA)
        case _ ⇒ new EditorPanelUI(NO_EXTENSION)
      }
    }
    
    editor.setCode(initCode, initHash)
    editor
  }

  def highlightedFile(ext: FileExtension): Option[HighlightedFile] =
    ext match {
      case OpenMOLEScript ⇒ Some(HighlightedFile("openmole"))
      case e: EditableFile ⇒ Some(HighlightedFile(e.highlighter))
      case _ ⇒ None
    }

  case class HighlightedFile(highlighter: String)

  @js.native
  @JSImport("ace-builds/src-noconflict/mode-openmole.js", JSImport.Namespace)
  object openmolemode extends js.Object
}

class EditorPanelUI(fileType: FileExtension) {

  val edDiv = div(idAttr := "editor", fontFamily := "monospace")
  val editor = {
    val ed = ace.edit(edDiv.ref)
    val session = ed.getSession()

    js.Dynamic.global.ace.config.set("basePath", "js")
    js.Dynamic.global.ace.config.set("modePath", "js")
    js.Dynamic.global.ace.config.set("themePath", "js")

    scalamode
    EditorPanelUI.openmolemode
    githubtheme
    extLanguageTools

    ed.setTheme("ace/theme/github")

    EditorPanelUI.highlightedFile(fileType).foreach { h ⇒
      session.setMode("ace/mode/" + h.highlighter)
    }

    ed.renderer.setShowGutter(true)
    ed.setShowPrintMargin(true)
    ed.setAutoScrollEditorIntoView(false)

    ed.setOptions(
      js.Dynamic.literal(
        "enableBasicAutocompletion" -> true,
        "enableSnippets" -> true,
        "enableLiveAutocompletion" -> true
      )
    )
    ed
  }

  var initialContentHash = ""

  def onSaved(hash: String) = initialContentHash = hash

  lazy val lineHeight = Var(15)

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
    errorsWithLocation.now().find {
      _.line == Some(editor.selection.getCursor().row.toInt)
    } match {
      case Some(e) =>
        val message = s"${
          e.line.map {
            _ + 1
          }.getOrElse("")
        }: ${e.stackTrace}"
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
        e.errorWithLocation.line.map { l ⇒
          editor.getSession().setBreakpoint(l)
        }
        e.errorWithLocation
      }
    }

    errorsWithLocation.set(ewls)
  }


  lazy val changed = Var(false)

  def updateFont(lHeight: Int) = {
    lineHeight.set(lHeight)
  }

  val view = {
    div(
      errorMessageOpen.signal.expand(div(
        flexRow,
        child.text <-- errorMessage.signal,
        backgroundColor := "#d35f5f", color := "white", height := "100", padding := "10", fontFamily := "gi")),
      edDiv.amend(
        lineHeight --> lineHeightObserver,
        fileType match {
          case FileExtension.OMS ⇒ errors --> omsErrorObserver
          case _ => emptyMod
        },
        onClick --> { _ =>
          setErrorMessage
        }
      )
    )
  }

  def aceDoc = editor.getSession().getDocument()

  def code = editor.synchronized {
    (editor.getSession().getValue(), initialContentHash)
  }

  def setCode(content: String, hash: String) = editor.synchronized {
    initialContentHash = hash
    editor.getSession().setValue(content)
  }

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)
}

