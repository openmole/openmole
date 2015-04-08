package org.openmole.gui.misc.js

import org.openmole.core.tools.service.Logger
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future
import scala.scalajs.js
import js.Dynamic.{ literal ⇒ lit }
import js.{ Dynamic ⇒ Dyn }
import scala.async.Async.{ async, await }
import fr.iscpif.scaladget.mapping.ace._
import fr.iscpif.scaladget.ace._

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

class OMEditor(bindings: Seq[(String, String, () ⇒ Any)],
               completions: () ⇒ Future[Seq[(String, String)]],
               implicit val logger: Logger) {
  lazy val Autocomplete = ace.require("ace/autocomplete").Autocomplete

  def sess = editor.getSession()

  def aceDoc = sess.getDocument()

  def code = sess.getValue().asInstanceOf[String]

  def row = editor.getCursorPosition().row.asInstanceOf[Int]

  def column = editor.getCursorPosition().column.asInstanceOf[Int]

  def complete() = {
    if (editor.completer == null)
      editor.completer = autocomplete
    js.Dynamic.global.window.ed = editor
    editor.completer.showPopup(editor)

    // needed for firefox on mac
    editor.completer.cancelContextMenu()
  }

  val editor: Editor = {
    val editor = OMEditor.initEditor

    for ((name, key, func) ← bindings) {
      val binding = s"Ctrl-$key|Cmd-$key"
      val command = editorCommand
      command.name = name
      command.bindKey = lit(
        "win" -> binding,
        "mac" -> binding,
        "sender" -> "editor|cli"
      )
      command.exec = func
      editor.commands.addCommand(
        command

      /*  lit(
        "name" -> name,
        "bindKey" -> lit(
          "win" -> binding,
          "mac" -> binding,
          "sender" -> "editor|cli"
        ),
        "exec" -> func
      )*/
      )
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

    editor
  }
}

object OMEditor {
  def initEditorIn(id: String) = {
    val editor = ace.edit(id)
    editor.setTheme("ace/theme/twilight")
    editor.renderer.setShowGutter(false)
    editor.setShowPrintMargin(false)
    editor
  }

  lazy val initEditor: Editor = {
    val editor = initEditorIn("editor")
    editor.getSession().setMode("ace/mode/scala")
    editor.getSession().setValue("Page.source.textContent")
    editor
  }
}