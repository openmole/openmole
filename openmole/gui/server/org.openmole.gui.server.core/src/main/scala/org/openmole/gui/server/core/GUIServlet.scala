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

import java.nio.ByteBuffer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{ FileService, FileServiceCache }
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.networkservice._
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.gui.ext.data.routes._
import org.openmole.gui.ext.server.{ OMRouter, utils }
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.file.*
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.{ RandomProvider, Seeder }
import org.openmole.tool.stream._
import org.openmole.tool.tar._

import scala.util.{ Failure, Success, Try }

object GUIServerServices {

  case class ServicesProvider(guiServices: GUIServerServices, cypherProvider: AtomicReference[Cypher]) extends Services {
    implicit def services: GUIServerServices = guiServices

    implicit def cypher: Cypher = cypherProvider.get()

    export guiServices.*
//    implicit def workspace: Workspace = guiServices.workspace
//
//    implicit def preference: Preference = guiServices.preference
//    implicit def threadProvider: ThreadProvider = guiServices.threadProvider
//
//    implicit def seeder: Seeder = guiServices.seeder
//
//    implicit def replicaCatalog: ReplicaCatalog = guiServices.replicaCatalog
//
//    implicit def tmpDirectory: TmpDirectory = guiServices.tmpDirectory
//
//    implicit def authenticationStore: AuthenticationStore = guiServices.authenticationStore
//
//    implicit def serializerService: SerializerService = guiServices.serializerService
//
//    implicit def fileService: FileService = guiServices.fileService
//    implicit def fileServiceCache = guiServices.fileServiceCache
//    implicit def randomProvider = guiServices.randomProvider
//    implicit def eventDispatcher: EventDispatcher = guiServices.eventDispatcher
//    implicit def networkService: NetworkService = guiServices.networkService
//    implicit def outputRedirection: OutputRedirection = guiServices.outputRedirection
//    implicit def loggerService: LoggerService = guiServices.loggerService
//    implicit def timeService: TimeService = guiServices.timeService
  }

  def apply(workspace: Workspace, httpProxy: Option[String], logLevel: Option[Level], logFileLevel: Option[Level]) = {
    implicit val ws: Workspace = workspace
    implicit val preference: Preference = Preference(ws.persistentDir)
    implicit val newFile: TmpDirectory = TmpDirectory(workspace)
    implicit val seeder: Seeder = Seeder()
    implicit val serializerService: SerializerService = SerializerService()
    implicit val threadProvider: ThreadProvider = ThreadProvider()
    implicit val authenticationStore: AuthenticationStore = AuthenticationStore(ws.persistentDir)
    implicit val fileService: FileService = FileService()
    implicit val randomProvider: RandomProvider = RandomProvider(seeder.newRNG)
    implicit val eventDispatcher: EventDispatcher = EventDispatcher()
    implicit val outputRedirection: OutputRedirection = OutputRedirection()
    implicit val networkService: NetworkService = NetworkService(httpProxy)
    implicit val fileServiceCache: FileServiceCache = FileServiceCache()
    implicit val replicaCatalog: ReplicaCatalog = ReplicaCatalog(ws)
    implicit val loggerService: LoggerService = LoggerService(logLevel, file = Some(workspace.location / Workspace.logLocation), fileLevel = logFileLevel)
    implicit val timeService: TimeService = TimeService()

    new GUIServerServices()
  }

  def dispose(services: GUIServerServices) = {
    scala.util.Try(Workspace.clean(services.workspace))
    scala.util.Try(services.threadProvider.stop())
  }

  def withServices[T](workspace: Workspace, httpProxy: Option[String], logLevel: Option[Level], logFileLevel: Option[Level])(f: GUIServerServices ⇒ T) = {
    val services = GUIServerServices(workspace, httpProxy, logLevel, logFileLevel)
    try f(services)
    finally dispose(services)
  }

}

class GUIServerServices(implicit
  val workspace:           Workspace,
  val preference:          Preference,
  val threadProvider:      ThreadProvider,
  val seeder:              Seeder,
  val replicaCatalog:      ReplicaCatalog,
  val tmpDirectory:             TmpDirectory,
  val authenticationStore: AuthenticationStore,
  val serializerService:   SerializerService,
  val fileService:         FileService,
  val fileServiceCache:    FileServiceCache,
  val randomProvider:      RandomProvider,
  val eventDispatcher:     EventDispatcher,
  val outputRedirection:   OutputRedirection,
  val networkService:      NetworkService,
  val loggerService:       LoggerService,
  val timeService:         TimeService
)

object GUIServlet {
//  def apply(arguments: GUIServer.ServletArguments) = {
//    val servlet = new GUIServlet(arguments)
//    Plugins.addPluginRoutes(servlet.addRouter, GUIServerServices.ServicesProvider(arguments.services, servlet.cypher.get))
//    servlet addRouter (OMRouter[Api](AutowireServer.route[Api](servlet.apiImpl)))
//    servlet
//  }

  def html(javascritMethod: String, cssFiles: Seq[String], extraHeader: String) = tags.html(
    tags.head(
      tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset=UTF-8"),
      cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := f) },
      tags.script(tags.`type` := "text/javascript", tags.src := "js/openmole-webpacked.js"),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/plotly.min.js"),
      //tags.script(tags.`type` := "text/javascript", tags.src := "js/" + utils.githubTheme),
      //tags.script(tags.`type` := "text/javascript", tags.src := "js/" + utils.openmoleGrammarMode),
      tags.link(tags.rel := "stylesheet", href := "https://cdn.jsdelivr.net/npm/bootstrap-icons@1.5.0/font/bootstrap-icons.css"),
      RawFrag(extraHeader)
    ),
    tags.body(
      tags.div(id := "openmole-content"),
      tags.script(javascritMethod)
    )
  )

  // Get all the css files in the workspace (it is not working with js because of the order)
  def cssFiles(webapp: File) = (webapp / "css").listFilesSafe.map { f => s"css/${f.getName}" }.sorted.toSeq

  val webpackLibrary = "openmole_library.openmole_library"
  val USER_ID = "UserID"

}
//
//class GUIServlet(val arguments: GUIServer.ServletArguments) extends ScalatraServlet with FileUploadSupport with AuthenticationSupport {
//  configureMultipartHandling(MultipartConfig(maxFileSize = Some(20 * 1024 * 1024 * 1024), fileSizeThreshold = Some(1024 * 1024))) // Limited to files of 20Go with 1Mo chunks
//
//  val cypher = new AtomicReference[Cypher](Cypher(arguments.password))
//  val services = GUIServerServices.ServicesProvider(arguments.services, cypher.get)
//  val apiImpl = new ApiImpl(services, Some(arguments.applicationControl))
//
//  import services._
//
//  //FIXME val connectedUsers: Var[Seq[UserID]] = Var(Seq())
//
//  def connection = GUIServlet.html(s"${GUIServlet.webpackLibrary}.connection();", GUIServlet.cssFiles(arguments.webapp), arguments.extraHeader)
//
//  def application = GUIServlet.html(s"${GUIServlet.webpackLibrary}.run();", GUIServlet.cssFiles(arguments.webapp), arguments.extraHeader)
//
//  def stopped = GUIServlet.html(s"${GUIServlet.webpackLibrary}.stopped();", GUIServlet.cssFiles(arguments.webapp), arguments.extraHeader)
//
//  def restarted = GUIServlet.html(s"${GUIServlet.webpackLibrary}.restarted();", GUIServlet.cssFiles(arguments.webapp), arguments.extraHeader)
//
//  def resetPassword = GUIServlet.html(s"${GUIServlet.webpackLibrary}.resetPassword();", GUIServlet.cssFiles(arguments.webapp), arguments.extraHeader)
//
//  def recordUser(u: UserID) = {
//    session.setAttribute(GUIServlet.USER_ID, u)
//    // connectedUsers() = connectedUsers.now :+ u
//  }
//
//  def userIDFromSession =
//    session.getAttribute(GUIServlet.USER_ID) match {
//      case u: UserID ⇒ Some(u)
//      case _         ⇒ None
//    }
//
//  protected def basicAuth(pass: String) = {
//    val baReq = new OpenMOLEtrategy(this, () ⇒ {
//      Preference.passwordIsCorrect(Cypher(pass), arguments.services.preference) && Preference.passwordChosen(arguments.services.preference)
//    })
//    val rep = baReq.authenticate()
//    rep match {
//      case Some(u: UserID) ⇒
//        response.setHeader("WWW-Authenticate", "OpenMOLE realm=\"%s\"" format realm)
//        // recordUser(u)
//        Ok()
//      case _ ⇒
//        redirect(slashConnectionRoute)
//    }
//  }
//
//  get(resetPasswordRoute) {
//    apiImpl.resetPassword()
//    resetPassword
//  }
//
//  post(resetPasswordRoute) {
//    val password = params.getOrElse("password", "")
//    val passwordAgain = params.getOrElse("passwordagain", "")
//
//    if (password == passwordAgain) {
//      Preference.setPasswordTest(arguments.services.preference, Cypher(password))
//      cypher.set(Cypher(password))
//    }
//
//    redirect(slashConnectionRoute)
//  }
//
//  get(shutdownRoute) {
//    stopped
//  }
//
//  get(restartRoute) {
//    restarted
//  }
//
//  post(uploadFilesRoute) {
//    def move(fileParams: FileSingleParams, fileType: String) = {
//
//      def copyTo(rootFile: File) =
//        for (file ← fileParams) {
//          val path = new java.net.URI(file._1).getPath
//          val destination = new File(rootFile, path)
//          destination.getParentFile.mkdirs()
//          destination.setWritable(true)
//          val stream = file._2.getInputStream
//          try {
//            stream.copy(destination)
//            destination.setExecutable(true)
//          }
//          finally stream.close
//        }
//
//      fileType match {
//        case "project"        ⇒ copyTo(utils.webUIDirectory)
//        case "authentication" ⇒ copyTo(utils.authenticationKeysFile)
//        case "plugin"         ⇒ copyTo(utils.pluginUpdoadDirectory(params("directoryName")))
//        case "absolute"       ⇒ copyTo(new File(""))
//      }
//    }
//
//    move(fileParams, params("fileType"))
//  }
//
//  get(downloadFileRoute) {
//    val path = params("path")
//    val hash = params.get("hash").flatMap(_.toBooleanOption).getOrElse(false)
//
//    val f = new File(utils.webUIDirectory, path)
//
//    if (!f.exists()) NotFound("The file " + path + " does not exist.")
//    else {
//      if (f.isDirectory) {
//        response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName + ".tgz"}"""")
//        val os = response.getOutputStream()
//        if (hash) response.setHeader(hashHeader, services.fileService.hashNoCache(f).toString)
//        val tos = new TarOutputStream(os.toGZ, 64 * 1024)
//        try tos.archive(f, includeTopDirectoryName = true)
//        finally tos.close
//      }
//      else {
//        f.withLock { _ ⇒
//          response.setHeader("Content-Disposition", s"""attachment; filename="${f.getName}"""")
//          response.setContentLengthLong(f.length)
//          if (hash) response.setHeader(hashHeader, services.fileService.hashNoCache(f).toString)
//          val os = response.getOutputStream()
//          try f.copy(os)
//          finally os.close
//        }
//      }
//    }
//  }
//
//  //  get(s"/${utils.openmoleGrammarMode}") {
//  //    redirect(s"/js/${utils.openmoleGrammarMode}")
//  //  }
//
//  get("/") {
//    redirect(slashAppRoute)
//  }
//
//  get(slashConnectionRoute) {
//    if (passwordIsChosen && passwordIsCorrect) redirect(slashAppRoute)
//    else if (passwordIsChosen) {
//      response.setHeader("Access-Control-Allow-Origin", "*")
//      response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
//      contentType = "text/html"
//      connection
//    }
//    else redirect(resetPasswordRoute)
//  }
//
//  post(slashConnectionRoute) {
//    response.setHeader("Access-Control-Allow-Origin", "*")
//    response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//    response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
//
//    val password = params.getOrElse("password", "")
//    cypher.set(Cypher(password))
//
//    passwordIsCorrect match {
//      case true ⇒ redirect(slashAppRoute)
//      case _    ⇒ redirect(slashConnectionRoute)
//    }
//  }
//
//  def passwordProvided = arguments.password.isDefined
//  def passwordIsChosen = Preference.passwordChosen(arguments.services.preference)
//  def passwordIsCorrect = Preference.passwordIsCorrect(cypher.get, arguments.services.preference)
//
//  get(slashAppRoute) {
//    contentType = "text/html"
//    if (!passwordIsChosen)
//      if (passwordProvided) Preference.setPasswordTest(preference, cypher.get)
//      else redirect(slashConnectionRoute)
//
//    if (passwordIsCorrect) application
//    else redirect(slashConnectionRoute)
//  }
//
//  def parseParams(toTest: Seq[String], evaluated: Map[String, String] = Map(), errors: Seq[Throwable] = Seq()): (Map[String, String], Seq[Throwable]) = {
//    if (toTest.isEmpty) (evaluated, errors)
//    else {
//      val testing = toTest.last
//      Try(params(testing)) match {
//        case Success(p) ⇒ parseParams(toTest.dropRight(1), evaluated + (testing → p), errors)
//        case Failure(e) ⇒ parseParams(toTest.dropRight(1), evaluated, errors :+ e)
//      }
//    }
//  }
//
//  def addRouter(router: OMRouter): Unit = {
//    post(s"/${router.route}/*") {
//      val is = request.getInputStream
//      val bytes: Array[Byte] = Iterator.continually(is.read()).takeWhile(_ != -1).map(_.asInstanceOf[Byte]).toArray[Byte]
//      val bb = ByteBuffer.wrap(bytes)
//
//      val rout =
//        router.router(
//          autowire.Core.Request(
//            router.route.split("/").toSeq ++ multiParams("splat").head.split("/"),
//            Unpickle[Map[String, ByteBuffer]].fromBytes(bb)
//          )
//        )
//
//      val req = Await.result(rout, Duration.Inf)
//      val data = Array.ofDim[Byte](req.remaining)
//      req.get(data)
//      Ok(data)
//    }
//  }
//}
//
//case class UserID(id: String)
//
//case class User(id: UserID, password: String)
//
//
//trait AuthenticationSupport extends ScentrySupport[User] with BasicAuthSupport[User] {
//  this: GUIServlet ⇒
//
//  val realm = "OpenMOLE (user name doesn't matter)"
//
//  protected def fromSession = {
//    case id: String ⇒ null
//  }
//
//  protected def toSession = {
//    case usr: User ⇒ usr.id.id
//  }
//
//  protected val scentryConfig = (new ScentryConfig {}).asInstanceOf[ScentryConfiguration]
//
//  override protected def configureScentry() = {
//    scentry.unauthenticated {
//      scentry.strategies("Basic").unauthenticated()
//    }
//  }
//
//  /*override protected def registerAuthStrategies = {
//    scentry.register("Basic", app ⇒ new OurBasicAuthStrategy(app, realm))
//  }*/
//
//}
