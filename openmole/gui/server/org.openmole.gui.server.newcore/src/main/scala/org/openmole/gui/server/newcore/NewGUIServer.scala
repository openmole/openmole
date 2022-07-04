package org.openmole.gui.server.newcore

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


import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.server.core.GUIServerServices
import cats.effect._
import org.http4s.blaze.server._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s._
import org.http4s.dsl.io._

class NewGUIServer(port: Int, localhost: Boolean, services: GUIServerServices) {

  val routes: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root => Ok("youpi")
  }

  def start() = {
    import cats.effect.unsafe.IORuntime
    implicit val runtime: IORuntime = cats.effect.unsafe.IORuntime.global

    val httpApp = Router("/" -> APIServer.routes).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(httpApp).allocated.unsafeRunSync()._2
  }


}
