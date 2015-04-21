package org.openmole.gui.client.core.dataui

import org.openmole.gui.ext.dataui.PanelUI
import org.scalajs.dom.raw.Event
import org.scalajs.jquery.jQuery
import rx._

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
import org.scalajs.dom
import org.scalajs.jquery.jQuery

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

  val editor: Var[Option[Editor]] = Var(init)

  lazy val Autocomplete = ace.require("ace/autocomplete").Autocomplete

  def save = {}

  override def jQueryCalls = () ⇒ {
    editor() = init

  }

  val view = {
    tags.div(id := "editorContainer", `class` := "container", width := "95%")(
      tags.div(`class` := "panel panel-default")(
        tags.div(`class` := "panel-body")(
          tags.div(id := "editor", onclick := { () ⇒
            println("ON click")
            init
          }
          )
        )
      )
    )
  } //EditorPanelUI.tag

  def set(c: String) = {
    println("SEST")
    init
    sess.map {
      _.setValue(c)
    }
  }

  def sess = editor().map {
    _.getSession()
  }

  def aceDoc = sess.map {
    _.getDocument()
  }

  def code: String = sess.map {
    _.getValue().asInstanceOf[String]
  }.getOrElse("")

  def complete() = editor().map {
    edit ⇒
      if (edit.completer == null)
        edit.completer = fr.iscpif.scaladget.ace.autocomplete
      js.Dynamic.global.window.ed = edit
      edit.completer.showPopup(edit)

      // needed for firefox on mac
      edit.completer.cancelContextMenu()
  }

  def init: Option[Editor] = {
    println("--------------------------------- INIT ... ")
    if (jQuery("#editor").length > 0) {
      println("------------------------------------ YES ! ")
      val edit = ace.edit("editor")
      edit.getSession().setMode("ace/mode/scala")
      edit.getSession().setValue(initCode)
      edit.setTheme("ace/theme/github")
      edit.renderer.setShowGutter(true)
      edit.setShowPrintMargin(true)

      for ((name, key, func) ← bindings) {
        val binding = s"Ctrl-$key|Cmd-$key"

        edit.commands.addCommand(
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

      def column = edit.getCursorPosition().column.asInstanceOf[Int]

      def row = edit.getCursorPosition().row.asInstanceOf[Int]

      def completions() = async {
        val code = edit.getSession().getValue().asInstanceOf[String]

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

      edit.completers = js.Array(lit(
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
      edit.getSession().setTabSize(2)
      Some(edit)
    }
    else None
  }

}

import scalatags.JsDom.all._

object EditorPanelUI {

  def apply(bindings: Seq[(String, String, () ⇒ Any)], initCode: String = "") = new EditorPanelUI(bindings, initCode)

  def tag = {
    val t = tags.div(`class` := "container", width := "95%")(
      tags.div(`class` := "panel panel-default")(
        tags.div(`class` := "panel-body")(
          tags.div(id := "editor")
        )
      )
    )
  }

}