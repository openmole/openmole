package org.openmole.gui.plugin.authentication.miniclust

/*
 * Copyright (C) 2023 Romain Reuillon
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


import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.core.services.Services

import org.openmole.gui.server.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*
import org.openmole.plugin.environment.miniclust.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*

import scala.util.{Failure, Success, Try}

object MiniclustAuthenticationRESTAPI:
  lazy val loginAuthentications: TapirEndpoint[Unit, Seq[MiniclustAuthenticationData]] =
    endpoint.get.in("miniclust" / "login-authentications").out(jsonBody[Seq[MiniclustAuthenticationData]]).errorOut(jsonBody[ErrorData])

  lazy val addAuthentication: TapirEndpoint[MiniclustAuthenticationData, Unit] =
    endpoint.post.in("miniclust" / "add-login-authentication").in(jsonBody[MiniclustAuthenticationData]).errorOut(jsonBody[ErrorData])

  lazy val removeAuthentication: TapirEndpoint[MiniclustAuthenticationData, Unit] =
    endpoint.post.in("miniclust" / "remove-login-authentication").in(jsonBody[MiniclustAuthenticationData]).errorOut(jsonBody[ErrorData])

  lazy val testAuthentication: TapirEndpoint[MiniclustAuthenticationData, Seq[Test]] =
    endpoint.post.in("miniclust" / "test-login-authentication").in(jsonBody[MiniclustAuthenticationData]).out(jsonBody[Seq[Test]]).errorOut(jsonBody[ErrorData])


class MiniclustAuthenticationServer(s: Services):

  val routes: HttpRoutes[IO] =
    routesFromEndpoints(
      MiniclustAuthenticationRESTAPI.loginAuthentications.implementedBy(impl.loginAuthentications),
      MiniclustAuthenticationRESTAPI.addAuthentication.implementedBy(impl.addAuthentication),
      MiniclustAuthenticationRESTAPI.removeAuthentication.implementedBy(impl.removeAuthentication),
      MiniclustAuthenticationRESTAPI.testAuthentication.implementedBy(impl.testAuthentication)
    )

  object impl:

    import s.*

    private def coreObject(data: MiniclustAuthenticationData) = MiniClustAuthentication.LoginPassword(
      data.url,
      data.login,
      data.password
    )

    def addAuthentication(data: MiniclustAuthenticationData): Unit = MiniClustAuthentication += coreObject(data)

    def removeAuthentication(data: MiniclustAuthenticationData): Unit = MiniClustAuthentication -= coreObject(data)

    def loginAuthentications(u: Unit): Seq[MiniclustAuthenticationData] = MiniClustAuthentication().flatMap:
      case lp: MiniClustAuthentication.LoginPassword =>
        Some:
          MiniclustAuthenticationData(
            lp.login,
            lp.password,
            lp.url
          )
      case _ => None

    def testAuthentication(data: MiniclustAuthenticationData): Seq[Test] =
      Seq(
        MiniClustAuthentication.test(coreObject(data)) match 
          case Success(_) => Test.passed()
          case Failure(f) => Test.error("failed", ErrorData(f))
      )



