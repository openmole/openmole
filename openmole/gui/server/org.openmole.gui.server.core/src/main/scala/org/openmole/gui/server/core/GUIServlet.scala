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

import org.scalatra._
import org.scalatra.auth.{ ScentryConfig, ScentryStrategy, ScentrySupport }
import org.scalatra.auth.strategy.BasicAuthSupport
import org.scalatra.servlet.{ FileItem, FileUploadSupport }
import org.scalatra.util.MultiMapHeadView

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.Api

import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.io.File

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data.OpenMOLEScript
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.tool.tar._
import rx.Var

import scala.util.{ Failure, Success, Try }

object AutowireServer extends autowire.Server[String, upickle.default.Reader, upickle.default.Writer] {
  def read[Result: upickle.default.Reader](p: String) = upickle.default.read[Result](p)

  def write[Result: upickle.default.Writer](r: Result) = upickle.default.write(r)
}

@MultipartConfig(fileSizeThreshold = 1024 * 1024)
class GUIServlet(val arguments: GUIServer.ServletArguments) extends ScalatraServlet with FileUploadSupport with AuthenticationSupport {

  val basePath = "org/openmole/gui/shared"
  val apiImpl = new ApiImpl(arguments)

  val connectionRoute = "/connection"
  val shutdownRoute = "/shutdown"
  val appRoute = "/app"
  val downloadFileRoute = "/downloadFile"
  val uploadFilesRoute = "/uploadFiles"
  val resetPasswordRoute = "/resetPassword"

  //FIXME val connectedUsers: Var[Seq[UserID]] = Var(Seq())
  val USER_ID = "UserID"

  def connection = html("ScriptClient().connection();")

  def application = html("ScriptClient().run();")

  def stopped = html("ScriptClient().stopped();")

  def resetPassword = html("ScriptClient().resetPassword();")

  def html(javascritMethod: String) = tags.html(
    tags.head(
      tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset=UTF-8"),
      cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
      tags.script(tags.`type` := "text/javascript", tags.src := "js/openmole.js") /*,
      tags.script(tags.`type` := "text/javascript", tags.src := "js/deps.js")*/
    ),
    tags.body(tags.onload := javascritMethod)
  )

  // Get all the css files in the workspace (it is not working with js because of the order)
  val cssFiles = new File(GUIServer.resourcePath, "css").listFilesSafe.map {
    _.getName
  }.sorted

  /* def isLoggedIn: Boolean =
    //FIXME
  userIDFromSession.map {
    connectedUsers.now.contains
  }.getOrElse(false)*/

  def recordUser(u: UserID) = {
    session.put(USER_ID, u)
    // connectedUsers() = connectedUsers.now :+ u
  }

  def userIDFromSession =
    session.getAttribute(USER_ID) match {
      case u: UserID ⇒ Some(u)
      case _         ⇒ None
    }

  protected def basicAuth(pass: String) = {
    val baReq = new OpenMOLEtrategy(this, () ⇒ {
      arguments.passwordCorrect(pass) && Workspace.passwordChosen
    })
    val rep = baReq.authenticate()
    rep match {
      case Some(u: UserID) ⇒
        response.setHeader("WWW-Authenticate", "OpenMOLE realm=\"%s\"" format realm)
        // recordUser(u)
        Ok()
      case _ ⇒
        redirect(connectionRoute)
    }
  }

  get(resetPasswordRoute) {
    Workspace.reset
    resetPassword
  }

  post(resetPasswordRoute) {
    val password = params.getOrElse("password", "")
    val passwordAgain = params.getOrElse("passwordagain", "")
    Utils.setPassword(password, passwordAgain)
    redirect(connectionRoute)
  }

  get(shutdownRoute) {
    stopped
  }

  post(uploadFilesRoute) {
    def move(fileParams: MultiMapHeadView[String, FileItem], fileType: String) = {
      def copyTo(rootFile: File) =
        for (file ← fileParams) yield {
          val path = new java.net.URI(file._1).getPath
          val destination = new File(rootFile, path)
          destination.getParentFile.mkdirs()
          destination.setWritable(true)
          val stream = file._2.getInputStream
          try {
            stream.copy(destination)
            destination.setExecutable(true)
          }
          finally stream.close
          destination
        }

      fileType match {
        case "project"        ⇒ copyTo(Utils.webUIProjectFile)
        case "authentication" ⇒ copyTo(Utils.authenticationKeysFile)
        case "plugin"         ⇒ copyTo(Utils.pluginUpdoadDirectory)
        case "absolute"       ⇒ copyTo(new File(""))
      }
    }

    move(fileParams, params("fileType"))
  }

  get(downloadFileRoute) {
    val path = new java.net.URI(null, null, params("path"), null).getPath
    val f = new File(Utils.webUIProjectFile, path)

    if (!f.exists()) NotFound("The file " + path + " does not exist.")
    else {
      if (f.isDirectory) {
        response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName + ".tgz"}"""")
        val os = response.getOutputStream()
        val tos = new TarOutputStream(os.toGZ, 64 * 1024)
        try tos.archive(f, includeTopDirectoryName = true)
        finally tos.close
      }
      else {
        response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName}"""")
        response.setContentLengthLong(f.length)
        val os = response.getOutputStream()
        try f.copy(os)
        finally os.close
      }
    }
  }

  get("/") {
    redirect(appRoute)
  }

  get(connectionRoute) {
    if (Workspace.passwordHasBeenSet) redirect("/app")
    else if (apiImpl.passwordState.chosen) {
      response.setHeader("Access-Control-Allow-Origin", "*")
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
      contentType = "text/html"
      connection
    }
    else redirect(resetPasswordRoute)
  }

  post(connectionRoute) {
    response.setHeader("Access-Control-Allow-Origin", "*")
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
    response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")

    val password = params.getOrElse("password", "")
    Utils.setPassword(password)

    Workspace.passwordHasBeenSet match {
      case true ⇒ redirect(appRoute)
      case _    ⇒ redirect(connectionRoute)
    }
  }

  get(appRoute) {
    contentType = "text/html"
    if (Workspace.passwordHasBeenSet) application
    else redirect(connectionRoute)
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
    Await.result(AutowireServer.route[Api](apiImpl)(
      autowire.Core.Request(
        basePath.split("/").toSeq ++ multiParams("splat").head.split("/"),
        upickle.default.read[Map[String, String]](request.body)
      )
    ), Duration.Inf)
  }
}

case class UserID(id: String)

case class User(id: UserID, password: String)

trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
  this: GUIServlet ⇒

  val realm = "OpenMOLE (user name doesn't matter)"

  protected def fromSession = {
    case id: String ⇒ null
  }

  protected def toSession = {
    case usr: User ⇒ usr.id.id
  }

  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]

  override protected def configureScentry = {
    scentry.unauthenticated {
      scentry.strategies("Basic").unauthenticated()
    }
  }

  /*override protected def registerAuthStrategies = {
    scentry.register("Basic", app ⇒ new OurBasicAuthStrategy(app, realm))
  }*/

}
