package org.openmole.gui.server.core

/*
 * Copyright (C) 22/09/14 // mathieu.leclaire@openmole.org
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

import cats.effect.IO
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.openmole.core.fileservice.FileService
import org.openmole.core.location.*
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.gui.server.jscompile.{JSPack, Webpack}
import org.openmole.tool.crypto.{Cypher, KeyStore}
import org.openmole.tool.file.*
import org.openmole.tool.network.Network
import cats.effect.*
import org.http4s.*
import org.http4s.blaze.server.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.openmole.core.logconfig.LoggerConfig
import org.openmole.core.networkservice.NetworkService
import org.openmole.gui.server.core.{ApiImpl, GUIServer, GUIServerServices}
import org.openmole.gui.server.ext.{GUIPluginRegistry, utils}
import org.openmole.tool.crypto.Cypher

import java.io.File
import java.util.concurrent.Semaphore
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.{Level, Logger}

object GUIServer:

  def fromWebAppLocation = openMOLELocation / "webapp"
  def webpackLocation = openMOLELocation / "webpack"

  def webapp(optimizedJS: Boolean)(using newFile: TmpDirectory, workspace: Workspace, fileService: FileService, networkService: NetworkService) =
    val from = fromWebAppLocation
    val to = newFile.newDir("webapp")

    val cssTarget = to / "css"
    from / "css" copy to / "css"
    from / "fonts" copy to / "fonts"
    from / "img" copy to / "img"

    val webpacked = Plugins.openmoleFile(optimizedJS)

    val jsTarget = to /> "js"

    webpacked copy (jsTarget / utils.webpakedOpenmoleFileName)
    new File(webpacked.getAbsolutePath + ".map") copy (to /> "js" / (webpacked.getName + ".map"))

    Plugins.persistentWebUI / utils.openmoleGrammarMode copy jsTarget / utils.openmoleGrammarMode
    Plugins.persistentWebUI / "node_modules/plotly.js/dist/plotly.min.js" copy jsTarget / "plotly.min.js"
    Plugins.persistentWebUI / "node_modules/ace-builds/src-min-noconflict/ace.js" copy jsTarget / "ace.js"
    Plugins.persistentWebUI / "node_modules/nouislider/dist/nouislider.min.js" copy jsTarget / "nouislider.min.js"
    Plugins.persistentWebUI / "node_modules/bootstrap-icons/font/" copy cssTarget / "bootstrap-icons"

    to


  lazy val port = PreferenceLocation("GUIServer", "Port", Some(Network.freePort))

  lazy val plugins = PreferenceLocation[Seq[String]]("GUIServer", "Plugins", None)

  def initialisePreference(preference: Preference) =
    if (!preference.isSet(port)) preference.setPreference(port, Network.freePort)

  def lockFile(implicit workspace: Workspace) = 
    val file = workspace.persistentDir / "GUI.lock"
    file.createNewFile
    file

  def urlFile(implicit workspace: Workspace) = workspace.persistentDir / "GUI.url"

  val servletArguments = "servletArguments"

  def waitingOpenMOLEContent = """
    |<html>
    |  <head>
    |    <div style="display:none;">launching-page</div>
    |    <script>
    |      setInterval(function(){
    |        fetch('gui/application/is-alive').then(r => r.text()).then((text) => { if(text.includes("true") && !text.includes("launching-page")) { window.location.reload(1); } })
    |      }, 3000);
    |    </script>
    |  </head>
    |  <link href="css/style.css" rel="stylesheet"/>
    |  <body>
    |    <div>
    |      OpenMOLE is launching...
    |      <div class="loader" style="float: right"></div><br/>
    |    </div>
    |    (for the first launch, and after an update, it may take several minutes)
    |  </body>
    |</html>""".stripMargin

  def waitRouter =
    import org.http4s.headers.{`Content-Type`}
    val routes: HttpRoutes[IO] = HttpRoutes.of:
      case _ =>  org.http4s.dsl.io.Ok.apply(waitingOpenMOLEContent).map(_.withContentType(`Content-Type`(MediaType.text.html)))

    Router("/" -> routes)

  case class ApplicationControl(stop: () ⇒ Unit)

  sealed trait ExitStatus
  case object Ok extends ExitStatus


  def apply(
    port: Int,
    localhost: Boolean,
    services: GUIServerServices,
    password: Option[String],
    optimizedJS: Boolean,
    extraHeaders: String) =

    Logger.getLogger(classOf[BlazeServerBuilder[?]].getName).setLevel(Level.WARNING)
    Logger.getLogger("org.http4s.blaze.channel.nio1.NIO1SocketServerGroup").setLevel(Level.WARNING)
    Logger.getLogger("org.http4s.blaze.channel.nio1.SelectorLoop").setLevel(Level.WARNING)

    import services.*
    implicit val runtime = cats.effect.unsafe.IORuntime.global

    val waitingServerShutdown = server(port, localhost).withHttpApp(waitRouter.orNotFound).allocated.unsafeRunSync()._2
    val webappCache =
      try GUIServer.webapp(optimizedJS)
      finally waitingServerShutdown.unsafeRunSync()
      
    new GUIServer(
      port,
      localhost,
      services,
      password,
      webappCache,
      extraHeaders
    )

  case class Control():
    var cancel: () => _ = null

    @volatile var exitStatus: GUIServer.ExitStatus = GUIServer.Ok
    val semaphore = new Semaphore(0)

    def join(): GUIServer.ExitStatus =
      semaphore.acquire()
      semaphore.release()
      exitStatus

    def stop() =
      cancel()
      semaphore.release()


  def server(port: Int, localhost: Boolean) =
    val s =
      if (localhost) BlazeServerBuilder[IO].bindHttp(port, "localhost")
      else BlazeServerBuilder[IO].bindHttp(port, "0.0.0.0")

    s
  end server


class GUIServer(
  port: Int,
  localhost: Boolean,
  services: GUIServerServices,
  password: Option[String],
  webappCache: File,
  extraHeaders: String):

  def start() =
    import cats.effect.unsafe.IORuntime
    import cats.effect.unsafe.IORuntimeConfig
    import cats.effect.unsafe.Scheduler

    val control = GUIServer.Control()
    val applicationControl =
      GUIServer.ApplicationControl(
        () ⇒ control.stop()
      )

    val serviceProvider = GUIServerServices.ServicesProvider(services)
    val apiImpl = new ApiImpl(serviceProvider, Some(applicationControl))
    apiImpl.activatePlugins

    import org.http4s.server.middleware.*

    val apiServer = new CoreAPIServer(apiImpl, utils.HTTP.stackError)
    val restServer = new RESTAPIv1Server(apiImpl)
    val webdavServer = new WebdavServer(org.openmole.gui.server.ext.utils.projectsDirectory(services.workspace))
    val applicationServer = new ApplicationServer(webappCache, extraHeaders, password, serviceProvider)


    //    implicit val runtime: IORuntime =
    //      cats.effect.unsafe.IORuntime(
    //        compute = services.threadProvider.executionContext,
    //        blocking = services.threadProvider.executionContext,
    //        scheduler = Scheduler.createDefaultScheduler()._1,
    //        shutdown = () => (),
    //        config = IORuntimeConfig()
    //      )

    val pluginsRoutes = apiImpl.pluginRoutes.map(r => s"/" -> GZip(r.router)).toSeq
    val httpApp = Router(
      Seq(
        s"/" -> GZip(applicationServer.routes),
        s"/" -> GZip(apiServer.routes),
        s"/" -> GZip(apiServer.endpointRoutes),
        s"/rest/v1" -> restServer.routes,
        s"/webdav" -> webdavServer.routes) ++ pluginsRoutes: _*).orNotFound

    implicit val runtime = cats.effect.unsafe.IORuntime.global

    val shutdown =
      GUIServer.
        server(port, localhost).
        withHttpApp(httpApp).
        withIdleTimeout(Duration.Inf).
        withResponseHeaderTimeout(Duration.Inf).
        withServiceErrorHandler(r => t => utils.HTTP.stackError(t)).
        allocated.unsafeRunSync()._2 // feRunSync()._2

    control.cancel = shutdown.unsafeRunSync
    control
  end start



