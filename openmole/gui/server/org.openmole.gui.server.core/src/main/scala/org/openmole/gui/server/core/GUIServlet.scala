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
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }

import org.openmole.core.workspace.Workspace
import org.openmole.gui.misc.utils.Utils._
import org.scalatra._
import org.scalatra.auth.{ ScentryConfig, ScentrySupport }
import org.scalatra.auth.strategy.{ BasicAuthSupport, BasicAuthStrategy }
import org.scalatra.servlet.{ FileItem, FileUploadSupport }
import org.scalatra.util.MultiMapHeadView
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.Api
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.io.{ BufferedOutputStream, File }
import org.openmole.tool.file._
import org.openmole.tool.tar._
import scala.util.{ Failure, Success, Try }

object AutowireServer extends autowire.Server[String, upickle.Reader, upickle.Writer] {
  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}

@MultipartConfig(fileSizeThreshold = 1024 * 1024)
class GUIServlet(val arguments: GUIServer.ServletArguments) extends ScalatraServlet with FileUploadSupport with AuthenticationSupport {

  val basePath = "org/openmole/gui/shared"

  // Get all the css files in the workspace (it is not working with js because of the order)
  val cssFiles = new File(GUIServer.resourcePath, "css").listFilesSafe.map {
    _.getName
  }.sorted

  before() {
    if (arguments.passwordCorrect.isDefined) basicAuth()
  }

  get("/shutdown") {
    val restart = Try(params("restart").toBoolean).getOrElse(false)
    if (restart) arguments.applicationControl.restart() else arguments.applicationControl.stop()
  }

  post("/uploadfiles") {
    move(fileParams, params("fileType"))
  }

  def move(fileParams: MultiMapHeadView[String, FileItem], fileType: String) = {
    def moveTo(rootFile: File) =
      for (file ← fileParams) yield {
        val path = new java.net.URI(file._1).getPath
        val destination = new File(rootFile, path)
        destination.setWritable(true)
        val stream = file._2.getInputStream
        try {
          println("cop to " + destination.getAbsolutePath)
          stream.copy(destination)
          destination.setExecutable(true)
        }
        finally stream.close
        destination
      }

    fileType match {
      case "project" ⇒ moveTo(Utils.webUIProjectFile)
      case "authentication" ⇒
        println("move to " + Utils.authenticationKeysFile)
        moveTo(Utils.authenticationKeysFile)
      case "plugin"   ⇒ ApiImpl.addPlugins(moveTo(Workspace.pluginDir))
      case "absolute" ⇒ moveTo(new File(""))
    }

  }

  get("/downloadFile") {
    val path = new java.net.URI(null, null, params("path"), null).getPath
    val f = new File(Utils.webUIProjectFile, path)

    if (!f.exists()) NotFound("The file " + path + " does not exist.")
    else {
      if (f.isDirectory) {
        response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName + ".tgz"}"""")
        val os = new BufferedOutputStream(response.getOutputStream())
        val tos = new TarOutputStream(os.toGZ)
        try tos.archive(f, includeTopDirectoryName = true)
        finally tos.close
      }
      else {
        response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName}"""")
        val os = new BufferedOutputStream(response.getOutputStream())
        try f.copy(os)
        finally os.close
      }
    }
  }

  get("/") {
    contentType = "text/html"
    tags.html(
      tags.head(
        tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset = ISO-8859-1"),
        cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
        tags.script(tags.`type` := "text/javascript", tags.src := "js/openmole.js"),
        tags.script(tags.`type` := "text/javascript", tags.src := "js/deps.js")
      ),
      tags.body(
        tags.onload := "ScriptClient().run();"
      )
    )
  }

  def parseParams(toTest: Seq[String], evaluated: Map[String, String] = Map(), errors: Seq[Throwable] = Seq()): (Map[String, String], Seq[Throwable]) = {
    if (toTest.isEmpty) (evaluated, errors)
    else {
      val testing = toTest.last
      Try(params(testing)) match {
        case Success(p) ⇒ parseParams(toTest.dropRight(1), evaluated + (testing → p), errors)
        case Failure(e) ⇒ parseParams(toTest.dropRight(1), evaluated, errors :+ e)
      }
    }
  }

  post(s"/$basePath/*") {
    Await.result(AutowireServer.route[Api](ApiImpl)(
      autowire.Core.Request(
        basePath.split("/").toSeq ++ multiParams("splat").head.split("/"),
        upickle.read[Map[String, String]](request.body)
      )
    ), Duration.Inf)
  }
}

case class User(id: String)

trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
  this: GUIServlet ⇒

  val realm = "OpenMOLE (user name doesn't matter)"

  protected def fromSession = {
    case id: String ⇒ User(id)
  }

  protected def toSession = {
    case usr: User ⇒ usr.id
  }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  override protected def configureScentry = {
    scentry.unauthenticated {
      scentry.strategies("Basic").unauthenticated()
    }
  }

  override protected def registerAuthStrategies = {
    scentry.register("Basic", app ⇒ new OurBasicAuthStrategy(app, realm))
  }

  class OurBasicAuthStrategy(protected override val app: ScalatraBase, realm: String)
      extends BasicAuthStrategy[User](app, realm) {

    override protected def validate(userName: String, password: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[User] =
      if (arguments.passwordCorrect.get(password)) Some(User(userName)) else None

    override protected def getUserId(user: User)(implicit request: HttpServletRequest, response: HttpServletResponse): String = user.id
  }

}
