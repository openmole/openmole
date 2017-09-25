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

import org.scalatra._
import org.scalatra.auth.{ ScentryConfig, ScentrySupport }
import org.scalatra.auth.strategy.BasicAuthSupport
import org.scalatra.servlet.{ FileItem, FileUploadSupport, MultipartConfig }
import org.scalatra.util.MultiMapHeadView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.io.File
import java.util.concurrent.atomic.AtomicReference

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.db.DBServerInfo
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.gui.ext.api.Api
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.gui.ext.data.routes._
import org.openmole.gui.ext.tool.server
import org.openmole.gui.ext.tool.server.{ AutowireServer, OMRouter }
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.file._
import org.openmole.tool.random.{ RandomProvider, Seeder }
import org.openmole.tool.stream._
import org.openmole.tool.tar._

import scala.util.{ Failure, Success, Try }

object GUIServices {

  case class ServicesProvider(guiServices: GUIServices, cypherProvider: () ⇒ Cypher) extends Services {
    implicit def services = guiServices
    implicit def workspace = guiServices.workspace
    implicit def preference = guiServices.preference
    implicit def cypher = cypherProvider()
    implicit def threadProvider = guiServices.threadProvider
    implicit def seeder = guiServices.seeder
    implicit def replicaCatalog = guiServices.replicaCatalog
    implicit def newFile = guiServices.newFile
    implicit def authenticationStore = guiServices.authenticationStore
    implicit def serializerService = guiServices.serializerService
    implicit def fileService = guiServices.fileService
    implicit def randomProvider = guiServices.randomProvider
    implicit def eventDispatcher: EventDispatcher = guiServices.eventDispatcher
  }

  def apply(workspace: Workspace) = {
    implicit val ws = workspace
    implicit val preference = Preference(ws.persistentDir)
    implicit val newFile = NewFile(workspace)
    implicit val seeder = Seeder()
    implicit val serializerService = SerializerService()
    implicit val threadProvider = ThreadProvider()
    implicit val replicaCatalog = ReplicaCatalog(ws)
    implicit val authenticationStore = AuthenticationStore(ws.persistentDir)
    implicit val fileService = FileService()
    implicit val randomProvider = RandomProvider(seeder.newRNG)
    implicit val eventDispatcher = EventDispatcher()

    new GUIServices()
  }

  def dispose(services: GUIServices) = {
    scala.util.Try(services.workspace.tmpDir.recursiveDelete)
    scala.util.Try(services.threadProvider.stop())
  }

  def withServices[T](workspace: Workspace)(f: GUIServices ⇒ T) = {
    val services = GUIServices(workspace)
    try f(services)
    finally dispose(services)
  }

}

class GUIServices(
  implicit
  val workspace:           Workspace,
  val preference:          Preference,
  val threadProvider:      ThreadProvider,
  val seeder:              Seeder,
  val replicaCatalog:      ReplicaCatalog,
  val newFile:             NewFile,
  val authenticationStore: AuthenticationStore,
  val serializerService:   SerializerService,
  val fileService:         FileService,
  val randomProvider:      RandomProvider,
  val eventDispatcher:     EventDispatcher
)

object GUIServlet {
  def apply(arguments: GUIServer.ServletArguments) = {
    val servlet = new GUIServlet(arguments)
    Utils.addPluginRoutes(servlet.addRouter, GUIServices.ServicesProvider(arguments.services, servlet.cypher.get))
    servlet
  }
}

class GUIServlet(val arguments: GUIServer.ServletArguments) extends ScalatraServlet with FileUploadSupport with AuthenticationSupport {
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(20 * 1024 * 1024 * 1024), fileSizeThreshold = Some(1024 * 1024))) // Limited to files of 20Go with 1Mo chunks

  val cypher = new AtomicReference[Cypher](Cypher(arguments.password))
  val services = GUIServices.ServicesProvider(arguments.services, cypher.get)
  val apiImpl = new ApiImpl(services, arguments.applicationControl)

  import services._

  //FIXME val connectedUsers: Var[Seq[UserID]] = Var(Seq())
  val USER_ID = "UserID"

  // def connection = html("OM.ScriptClient().connection();")

  //  def application = html("OM.ScriptClient().run();")

  // def stopped = html("OM.ScriptClient().stopped();")

  //  def resetPassword = html("OM.ScriptClient().resetPassword();")
  def connection = html("connection();")

  def application = html("run();")

  def stopped = html("stopped();")

  def restarted = html("restarted();")

  def resetPassword = html("resetPassword();")

  def html(javascritMethod: String) = tags.html(
    tags.head(
      tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset=UTF-8"),
      cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := "css/" + f) },
      tags.script(tags.`type` := "text/javascript", tags.src := "js/" + Utils.openmoleFileName),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/deps.js"),
      RawFrag(arguments.extraHeader)
    ),
    tags.body(tags.onload := javascritMethod)
  )

  // Get all the css files in the workspace (it is not working with js because of the order)
  val cssFiles = (arguments.webapp / "css").listFilesSafe.map {
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
      Preference.passwordIsCorrect(Cypher(pass), arguments.services.preference) && Preference.passwordChosen(arguments.services.preference)
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
    apiImpl.resetPassword()
    resetPassword
  }

  post(resetPasswordRoute) {
    val password = params.getOrElse("password", "")
    val passwordAgain = params.getOrElse("passwordagain", "")

    if (password == passwordAgain) {
      Preference.setPasswordTest(arguments.services.preference, Cypher(password))
      cypher.set(Cypher(password))
    }

    redirect(connectionRoute)
  }

  get(shutdownRoute) {
    stopped
  }

  get(restartRoute) {
    restarted
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
        case "project"        ⇒ copyTo(Utils.webUIDirectory)
        case "authentication" ⇒ copyTo(server.Utils.authenticationKeysFile)
        case "plugin"         ⇒ copyTo(Utils.pluginUpdoadDirectory)
        case "absolute"       ⇒ copyTo(new File(""))
      }
    }

    move(fileParams, params("fileType"))
  }

  get(downloadFileRoute) {
    val path = new java.net.URI(null, null, params("path"), null).getPath
    val f = new File(Utils.webUIDirectory, path)

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
    if (passwordIsChosen && passwordIsCorrect) redirect("/app")
    else if (passwordIsChosen) {
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
    cypher.set(Cypher(password))

    passwordIsCorrect match {
      case true ⇒ redirect(appRoute)
      case _    ⇒ redirect(connectionRoute)
    }
  }

  def passwordProvided = arguments.password.isDefined
  def passwordIsChosen = Preference.passwordChosen(arguments.services.preference)
  def passwordIsCorrect = Preference.passwordIsCorrect(cypher.get, arguments.services.preference)

  get(appRoute) {
    contentType = "text/html"
    if (!passwordIsChosen)
      if (passwordProvided) Preference.setPasswordTest(preference, cypher.get)
      else redirect(connectionRoute)

    if (passwordIsCorrect) application
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

  addRouter(OMRouter[Api](AutowireServer.route[Api](apiImpl)))

  def addRouter(router: OMRouter): Unit = {
    val bp = classOf[org.openmole.gui.ext.api.Api].getPackage.getName.replace('.', '/')
    post(s"/${router.route}/*") {
      val coreRequest = autowire.Core.Request(
        router.route.split("/").toSeq ++ multiParams("splat").head.split("/"),
        upickle.default.read[Map[String, String]](request.body)
      )
      Await.result(router.router(coreRequest), Duration.Inf)
    }
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
