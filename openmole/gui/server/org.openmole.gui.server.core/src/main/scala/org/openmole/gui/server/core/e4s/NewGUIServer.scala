package org.openmole.gui.server.core.e4s

/*
 * Copyright (C) 2022 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


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

object NewGUIServer {
  def apply(port: Int, localhost: Boolean, services: GUIServerServices, password: Option[String], optimizedJS: Boolean, extraHeaders: String) = {
    import services.*
    val webappCache = GUIServer.webapp(optimizedJS)
    new NewGUIServer(
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

class NewGUIServer(port: Int, localhost: Boolean, services: GUIServerServices, password: Option[String], webappCache: File, extraHeaders: String) {


  def start() = {
    import cats.effect.unsafe.IORuntime
    import cats.effect.unsafe.IORuntimeConfig
    import cats.effect.unsafe.Scheduler


    val control = NewGUIServer.Control()
    val applicationControl =
      GUIServer.ApplicationControl(
        () ⇒ {
          control.exitStatus = GUIServer.Restart
          control.stop()
        },
        () ⇒ control.stop()
      )

    val apiImpl = new ApiImpl(GUIServerServices.ServicesProvider(services, () => Cypher(password)), Some(applicationControl))
    val apiServer = new CoreAPIServer(apiImpl)
    val applicationServer = new ApplicationServer(webappCache, extraHeaders, password, services)


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

    val shutdown =
      BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(httpApp).allocated.unsafeRunSync()._2 // feRunSync()._2

    control.cancel = shutdown.unsafeRunSync
    control
  }



}
