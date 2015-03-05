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

import org.openmole.core.workspace.Workspace
import org.scalatra._
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.Api
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.io.File

object AutowireServer extends autowire.Server[String, upickle.Reader, upickle.Writer] {
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

class GUIServlet extends ScalatraServlet {

  val basePath = "org/openmole/gui/shared"

  // Get all the css files in the workspace (it is not working with js because of the order)
  val cssFiles = new File(Workspace.file("webui"), "webapp/css").listFiles.map {
    _.getName
  }.sorted

  get("/plugins.js.map") {
    contentType = "text/javascript"
    val webui = Workspace.file("webui")
    val webapp = new File(webui, "webapp")
    val jsSrc = new File(webapp, "js/plugins.js.map")
    response.setHeader("Content-Disposition", "attachment; filename=" + jsSrc.getName)
    jsSrc
  }

  get("/") {
    contentType = "text/html"
    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset = ISO-8859-1"),
        cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
        tags.script(tags.`type` := "text/javascript", tags.src := "js/jquery-2.1.3.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/d3.v3.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap-3.3.2.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/plugins.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/pluginMapping.js")
      ),
      tags.body(
        tags.onload := "fillMap();GUIClient().run();"
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
