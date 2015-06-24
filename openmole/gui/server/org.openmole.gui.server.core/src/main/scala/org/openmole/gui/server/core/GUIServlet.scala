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

import javax.servlet.annotation.MultipartConfig

import org.openmole.console.ConsoleVariables
import org.openmole.core.workflow.mole.ExecutionContext
import org.openmole.core.workflow.puzzle.Puzzle
import org.openmole.core.workspace.Workspace
import org.openmole.gui.misc.utils.Utils._
import org.scalatra._
import org.scalatra.servlet.FileUploadSupport
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.Api
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.io.{ File, PrintStream }
import org.openmole.tool.file._
import org.openmole.tool.tar._
import org.openmole.console._
import scala.util.{ Failure, Success, Try }
import org.openmole.gui.ext.data._

object AutowireServer extends autowire.Server[String, upickle.Reader, upickle.Writer] {
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

@MultipartConfig(fileSizeThreshold = 1024 * 1024)
class GUIServlet extends ScalatraServlet with FileUploadSupport {

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

  get("/gui") {
    contentType = "text/html"
    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset = ISO-8859-1"),
        cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
        tags.script(tags.`type` := "text/javascript", tags.src := "js/jquery.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/ace.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/d3.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/ace.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/mode-scala.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/mode-sh.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/theme-github.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/plugins.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/pluginMapping.js")
      ),
      tags.body(
        tags.onload := "fillMap();GUIClient().run();"
      )
    )
  }

  post("/uploadfiles") {
    for (file ← fileParams) yield {
      val stream = file._2.getInputStream
      try stream.copy(new java.io.File(file._1))
      finally stream.close
      Ok(file, Map(
        "Content-Type" -> ("application/octet-stream"),
        "Content-Disposition" -> ("form-data; filename=\"" + file._1 + "\"")
      ))
    }
  }

  post("/downloadedfiles") {
    val path = params("path")
    val file = new File(path)
    println("path " + path + " ///  " + file.getName)
    if (file.isDirectory) file.archiveCompress(new File(file.getName + ".tar.gz"))

    if (params("saveFile").toBoolean) {
      Ok(file, Map(
        "Content-Type" -> ("application/octet-stream"),
        "Content-Disposition" -> ("attachment; filename=\"" + file.getName + "\"")
      ))
    }
    else if (file.exists) {
      contentType = "application/octet-stream"
      Ok(file)
    }
    else NotFound("The file " + path + " does not exist.")
  }

  get("/") {
    contentType = "text/html"
    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset = ISO-8859-1"),
        cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
        tags.script(tags.`type` := "text/javascript", tags.src := "js/jquery.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/ace.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/mode-sh.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/mode-nlogo.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/theme-github.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/bootstrap.min.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/plugins.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/pluginMapping.js")
      ),
      tags.body(
        tags.onload := "fillMap();ScriptClient().run();")
    )
  }

  def parseParams(toTest: Seq[String], evaluated: Map[String, String] = Map(), errors: Seq[Throwable] = Seq()): (Map[String, String], Seq[Throwable]) = {
    if (toTest.isEmpty) (evaluated, errors)
    else {
      val testing = toTest.last
      Try(params(testing)) match {
        case Success(p) ⇒ parseParams(toTest.dropRight(1), evaluated + (testing -> p), errors)
        case Failure(e) ⇒ parseParams(toTest.dropRight(1), evaluated, errors :+ e)
      }
    }
  }

  post(s"/$basePath/*") {
    Await.result(AutowireServer.route[Api](ApiImpl)(
      autowire.Core.Request(basePath.split("/").toSeq ++ multiParams("splat").head.split("/"),
        upickle.read[Map[String, String]](request.body))
    ), Duration.Inf)
  }
}
