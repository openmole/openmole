/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
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
package org.openmole.gui.server.core

import java.util.UUID

import org.scalatra._
import scala.concurrent.ExecutionContext.Implicits.global
import upickle._
import autowire._
import org.openmole.gui.shared.Api
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import org.openmole.gui.server.factory.ServerFactories

object AutowireServer extends autowire.Server[String, upickle.Reader, upickle.Writer] {
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

class GUIServlet extends ScalatraServlet {

  println("in GUIServlet ...")
  println("READ : " + upickle.read[Seq[Int]]("[1, 4, 55]"))
  val basePath = "org/openmole/gui/shared"

  get("/") {
    contentType = "text/html"
    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "Content-Type", tags.content := "text/html; charset=UTF-8"),
        tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/workflow.css"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/d3.v3.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/plugins.js") /*,
        tags.script(tags.`type` := "text/javascript", tags.src := "js/plugins-opt.js"*/

      ),
      tags.body(tags.h1("OpenMOLE!  "),
        tags.onload := "GUIClient().run();"
      )
    )
  }

  post(s"/$basePath/*") {
    Await.result(AutowireServer.route[Api](ApiImpl)(
      autowire.Core.Request(basePath.split("/").toSeq ++ multiParams("splat").head.split("/"),
        upickle.read[Map[String, String]](request.body))
    ), Duration.Inf)
  }
}
