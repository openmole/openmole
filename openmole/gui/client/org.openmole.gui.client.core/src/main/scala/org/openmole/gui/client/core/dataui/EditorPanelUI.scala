package org.openmole.gui.client.core.dataui

import org.openmole.gui.ext.dataui.PanelUI

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal ⇒ lit }
import scala.scalajs.js.{ Dynamic ⇒ Dyn }
import scalatags.JsDom.all._
import scalatags.JsDom.tags
import scala.async.Async.{ async, await }
import fr.iscpif.scaladget.ace._
import fr.iscpif.scaladget.mapping.ace._

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

class EditorPanelUI(bindings: Seq[(String, String, () ⇒ Any)], initCode: String) extends PanelUI {

  lazy val Autocomplete = ace.require("ace/autocomplete").Autocomplete

  def save = {}

  val editorDiv = tags.div(id := "editor")
  val editor = ace.edit(editorDiv.render)
  initEditor

  val view = {
    tags.div(id := "editorContainer", `class` := "container", width := "95%")(
      tags.div(`class` := "panel panel-default")(
        tags.div(`class` := "panel-body")(
          editor.container
        )
      )
    )
  }

  def sess = editor.getSession()

  def aceDoc = sess.getDocument()

  def code: String = sess.getValue()

  def complete() = {
    if (editor.completer == null)
      editor.completer = fr.iscpif.scaladget.ace.autocomplete
    js.Dynamic.global.window.ed = editor
    editor.completer.showPopup(editor)

    // needed for firefox on mac
    editor.completer.cancelContextMenu()
  }

  def initEditor = {
    editor.getSession().setMode("ace/mode/scala")
    editor.getSession().setValue(initCode)
    editor.setTheme("ace/theme/github")
    editor.renderer.setShowGutter(true)
    editor.setShowPrintMargin(true)

    for ((name, key, func) ← bindings) {
      val binding = s"Ctrl-$key|Cmd-$key"

      editor.commands.addCommand(
        lit(
          "name" -> name,
          "bindKey" -> lit(
            "win" -> binding,
            "mac" -> binding,
            "sender" -> "editor|cli"
          ),
          "exec" -> func
        )
      )
    }

    def column = editor.getCursorPosition().column.asInstanceOf[Int]

    def row = editor.getCursorPosition().row.asInstanceOf[Int]

    def completions() = async {
      val code = editor.getSession().getValue().asInstanceOf[String]

      val intOffset = column + code.split("\n")
        .take(row)
        .map(_.length + 1)
        .sum

      val flag = if (code.take(intOffset).endsWith(".")) "member" else "scope"

      //FIXME: CALL FOR COMPILATION AND  COMPLETION
      val res = await(Future {
        println("fixme: comeletestuff and completion")
        List(("ss", "auie"))
      })
      //await(Post[Api].completeStuff(code, flag, intOffset).call())
      //  log("Done")
      //  logln()
      res
    }

    editor.completers = js.Array(lit(
      "getCompletions" -> { (editor: Editor, session: IEditSession, pos: Dyn, prefix: Dyn, callback: Dyn) ⇒
        async {
          val things = await(completions()).map {
            case (name, value) ⇒
              lit(
                "value" -> value,
                "caption" -> (value + name)
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

  def apply(bindings: Seq[(String, String, () ⇒ Any)], initCode: String = "") = new EditorPanelUI(bindings, initCode)

}