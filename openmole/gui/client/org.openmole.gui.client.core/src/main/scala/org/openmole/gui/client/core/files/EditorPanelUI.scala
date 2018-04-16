package org.openmole.gui.client.core.files

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal ⇒ lit }
import scala.scalajs.js.{ Dynamic ⇒ Dyn }
import scalatags.JsDom.all._
import scalatags.JsDom.tags
import scala.async.Async.{ async, await }
import scaladget.ace._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._

import org.openmole.gui.ext.tool.client._

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

class EditorPanelUI(initCode: String, fileType: FileExtension, containerModifierSeq: ModifierSeq) {

  def save(onsave: () ⇒ Unit) = {}

  val editorDiv = tags.div(id := "editor")
  val editor = ace.edit(editorDiv.render)

  val view = {
    div(editorContainer +++ container +++ containerModifierSeq)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          editor.container
        )
      )
    )
  }

  def sess = editor.getSession()

  def aceDoc = sess.getDocument()

  def code: String = sess.getValue()

  def setCode(content: String) = editor.getSession().setValue(content)

  def setReadOnly(b: Boolean) = editor.setReadOnly(b)

  def initEditor = {
    fileType match {
      case ef: EditableFile ⇒ editor.getSession().setMode("ace/mode/" + ef.highlighter)
      case _                ⇒
    }

    setCode(initCode)
    editor.setTheme("ace/theme/github")
    editor.renderer.setShowGutter(true)
    editor.setShowPrintMargin(true)
    editor.setAutoScrollEditorIntoView(true)

    // editor.getSession().setTabSize(2)
  }

}

object EditorPanelUI {

  def apply(fileType: FileExtension, initCode: String, containerModifierSeq: ModifierSeq = emptyMod) = fileType match {
    case OMS   ⇒ editor(initCode, OMS, containerModifierSeq)
    case SCALA ⇒ editor(initCode, SCALA, containerModifierSeq)
    case _     ⇒ empty(initCode, containerModifierSeq)
  }

  def empty(initCode: String, containerModifierSeq: ModifierSeq) = new EditorPanelUI(initCode, NO_EXTENSION, containerModifierSeq)

  private def editor(initCode: String = "", language: FileExtension, containerModifierSeq: ModifierSeq = emptyMod) = new EditorPanelUI(initCode, language, containerModifierSeq)

  def sh(initCode: String = "") = EditorPanelUI(SH, initCode)

}