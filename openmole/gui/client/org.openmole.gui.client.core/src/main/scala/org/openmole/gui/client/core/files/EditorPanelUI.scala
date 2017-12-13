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

class EditorPanelUI(bindings: Seq[(String, String, () ⇒ Any)], initCode: String, fileType: FileExtension) {

  lazy val Autocomplete = ace.require("ace/autocomplete").Autocomplete

  def save(onsave: () ⇒ Unit) = {}

  val editorDiv = tags.div(id := "editor")
  val editor = ace.edit(editorDiv.render)
  initEditor

  val view = {
    div(editorContainer +++ container)(
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

  def getScrollPostion = sess.getScrollTop

  def setScrollPosition(pos: Double) = sess.setScrollTop(pos)

  def complete() = {
    if (editor.completer == null)
      editor.completer = scaladget.ace.autocomplete //scaladget.ace.autocomplete
    js.Dynamic.global.window.ed = editor
    editor.completer.showPopup(editor)

    // needed for firefox on mac
    editor.completer.cancelContextMenu()
  }

  def initEditor = {
    fileType match {
      case ef: EditableFile ⇒ editor.getSession().setMode("ace/mode/" + ef.highlighter)
      case _                ⇒
    }

    setCode(initCode)
    editor.setTheme("ace/theme/github")
    editor.renderer.setShowGutter(true)
    editor.setShowPrintMargin(true)

    for ((name, key, func) ← bindings) {
      val binding = s"Ctrl-$key|Cmd-$key"

      editor.commands.addCommand(
        lit(
          "name" → name,
          "bindKey" → lit(
            "win" → binding,
            "mac" → binding,
            "sender" → "editor|cli"
          ),
          "exec" → func
        )
      )
    }

    def column = editor.getCursorPosition().column.asInstanceOf[Int]

    def row = editor.getCursorPosition().row.asInstanceOf[Int]

    def completions() = async {
      val code = editor.getSession().getValue()

      val intOffset = column + code.split("\n")
        .take(row)
        .map(_.length + 1)
        .sum

      //  val flag = if (code.take(intOffset).endsWith(".")) "member" else "scope"

      //FIXME: CALL FOR COMPILATION AND  COMPLETION
      val res = await(Future {
        List(("ss", "auie"))
      })
      //await(Post[Api].completeStuff(code, flag, intOffset).call())
      //  log("Done")
      //  logln()
      res
    }

    editor.completers = js.Array(lit(
      "getCompletions" → { (editor: Editor, session: IEditSession, pos: Dyn, prefix: Dyn, callback: Dyn) ⇒
        async {
          val things = await(completions()).map {
            case (name, value) ⇒
              lit(
                "value" → value,
                "caption" → (value + name)
              ).value
          }
          callback(null, js.Array(things: _*))
        }
      }
    ).value)
    editor.getSession().setTabSize(2)
  }

}

object EditorPanelUI {

  def apply(fileType: FileExtension, initCode: String) = fileType match {
    case OMS   ⇒ editor(initCode, OMS)
    case SCALA ⇒ editor(initCode, SCALA)
    case _     ⇒ empty(initCode)
  }

  def empty(initCode: String) = new EditorPanelUI(Seq(), initCode, NO_EXTENSION)

  private def editor(initCode: String = "", language: FileExtension) = new EditorPanelUI(Seq(
    ("Compile", "Enter", () ⇒ println("Compile  !"))
  ), initCode, language)

  def sh(initCode: String = "") = new EditorPanelUI(Seq(), initCode, SH)

}