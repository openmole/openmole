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

import java.util.concurrent.Semaphore
import org.openmole.core.fileservice.FileService
import org.openmole.core.location.*
import org.openmole.core.preference.{Preference, PreferenceLocation}
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.gui.ext.server.utils
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
import org.openmole.gui.ext.server.GUIPluginRegistry
import org.openmole.gui.server.core.{ApiImpl, GUIServer, GUIServerServices}
import org.openmole.tool.crypto.Cypher

import java.io.File
import java.util.concurrent.Semaphore
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

import java.io.File

object GUIServer {

  def fromWebAppLocation = openMOLELocation / "webapp"

  def webpackLocation = openMOLELocation / "webpack"

  def webapp(optimizedJS: Boolean)(implicit newFile: TmpDirectory, workspace: Workspace, fileService: FileService) = {
    val from = fromWebAppLocation
    val to = newFile.newDir("webapp")

    from / "css" copy to / "css"
    from / "fonts" copy to / "fonts"
    from / "img" copy to / "img"

    val webpacked = Plugins.openmoleFile(optimizedJS)

    val jsTarget = to /> "js"
    webpacked copy (jsTarget / utils.webpakedOpenmoleFileName)

    new File(webpacked.getAbsolutePath + ".map") copy (to /> "js" / (webpacked.getName + ".map"))

    to
  }

  lazy val port = PreferenceLocation("GUIServer", "Port", Some(Network.freePort))

  lazy val plugins = PreferenceLocation[String]("GUIServer", "Plugins", None)

  def initialisePreference(preference: Preference) = {
    if (!preference.isSet(port)) preference.setPreference(port, Network.freePort)
  }

  def lockFile(implicit workspace: Workspace) = {
    val file = utils.webUIDirectory / "GUI.lock"
    file.createNewFile
    file
  }

  def urlFile(implicit workspace: Workspace) = utils.webUIDirectory / "GUI.url"

  val servletArguments = "servletArguments"

  case class ServletArguments(
    services:           GUIServerServices,
    password:           Option[String],
    applicationControl: ApplicationControl,
    webapp:             File,
    extraHeader:        String,
    subDir:             Option[String]
  )

//  def waitingOpenMOLEContent =
//    <html>
//      <head>
//        <script>
//          { """setTimeout(function(){ window.location.reload(1); }, 3000);""" }
//        </script>
//      </head>
//      <link href="/css/style.css" rel="stylesheet"/>
//      <body>
//        <div>
//          OpenMOLE is launching...
//          <div class="loader" style="float: right"></div><br/>
//        </div>
//        (for the first launch, and after an update, it may take several minutes)
//      </body>
//    </html>

  case class ApplicationControl(restart: () ⇒ Unit, stop: () ⇒ Unit)

  sealed trait ExitStatus
  case object Restart extends ExitStatus
  case object Ok extends ExitStatus


  def apply(port: Int, localhost: Boolean, services: GUIServerServices, password: Option[String], optimizedJS: Boolean, extraHeaders: String) = {
    import services.*
    val webappCache = GUIServer.webapp(optimizedJS)
    new GUIServer(
      port,
      localhost,
      services,
      password,
      webappCache,
      extraHeaders
    )
  }

  case class Control() {
    var cancel: () => _ = null

    @volatile var exitStatus: GUIServer.ExitStatus = GUIServer.Ok
    val semaphore = new Semaphore(0)

    def join(): GUIServer.ExitStatus = {
      semaphore.acquire()
      semaphore.release()
      exitStatus
    }

    def stop() = {
      cancel()
      semaphore.release()
    }

  }
}


class GUIServer(port: Int, localhost: Boolean, services: GUIServerServices, password: Option[String], webappCache: File, extraHeaders: String) {


  def start() = {
    import cats.effect.unsafe.IORuntime
    import cats.effect.unsafe.IORuntimeConfig
    import cats.effect.unsafe.Scheduler


    val control = GUIServer.Control()
    val applicationControl =
      GUIServer.ApplicationControl(
        () ⇒ {
          control.exitStatus = GUIServer.Restart
          control.stop()
        },
        () ⇒ control.stop()
      )

    val serviceProvider = GUIServerServices.ServicesProvider(services, () => Cypher(password))
    val apiImpl = new ApiImpl(serviceProvider, Some(applicationControl))
    val apiServer = new CoreAPIServer(apiImpl)
    val applicationServer = new ApplicationServer(webappCache, extraHeaders, password, serviceProvider)


    //    implicit val runtime: IORuntime =
    //      cats.effect.unsafe.IORuntime(
    //        compute = services.threadProvider.executionContext,
    //        blocking = services.threadProvider.executionContext,
    //        scheduler = Scheduler.createDefaultScheduler()._1,
    //        shutdown = () => (),
    //        config = IORuntimeConfig()
    //      )

    val pluginsRoutes = apiImpl.pluginRoutes.map(r => "/" -> r.router).toSeq
    val httpApp = Router(Seq("/" -> applicationServer.routes, "/" -> apiServer.routes, "/" -> apiServer.endpointRoutes) ++ pluginsRoutes: _*).orNotFound

    implicit val runtime = cats.effect.unsafe.IORuntime.global
    
    def server =
      if(localhost) BlazeServerBuilder[IO].bindHttp(port, "localhost")
      else BlazeServerBuilder[IO].bindHttp(port, "0.0.0.0")

    val shutdown = server.withHttpApp(httpApp).allocated.unsafeRunSync()._2 // feRunSync()._2

    control.cancel = shutdown.unsafeRunSync
    control
  }



}

