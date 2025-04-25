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

import scala.util.{Failure, Success, Try}

trait MiniclustAuthenticationRESTAPI extends RESTAPI:
  val loginAuthentications: ErrorEndpoint[Unit, Seq[MiniclustAuthenticationData]] =
    errorEndpoint(get(path / "miniclust" / "login-authentications"), ok(jsonResponse[Seq[MiniclustAuthenticationData]]))

  val addAuthentication: ErrorEndpoint[MiniclustAuthenticationData, Unit] =
    errorEndpoint(post(path / "miniclust" / "add-login-authentication", jsonRequest[MiniclustAuthenticationData]), ok(jsonResponse[Unit]))

  val removeAuthentication: ErrorEndpoint[MiniclustAuthenticationData, Unit] =
    errorEndpoint(post(path / "miniclust" / "remove-login-authentication", jsonRequest[MiniclustAuthenticationData]), ok(jsonResponse[Unit]))

  val testAuthentication: ErrorEndpoint[MiniclustAuthenticationData, Seq[Test]] =
    errorEndpoint(post(path / "miniclust" / "test-login-authentication", jsonRequest[MiniclustAuthenticationData]), ok(jsonResponse[Seq[Test]]))


class MiniclustAuthenticationServer(s: Services) extends APIServer with MiniclustAuthenticationRESTAPI:
  
  type EFfect = super.Effect
  
  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      loginAuthentications.errorImplementedBy(impl.loginAuthentications),
      addAuthentication.errorImplementedBy(impl.addAuthentication),
      removeAuthentication.errorImplementedBy(impl.removeAuthentication),
      testAuthentication.errorImplementedBy(impl.testAuthentication)
    )
  )

  object impl:

    import s.*

    private def coreObject(data: MiniclustAuthenticationData) = MiniclustAuthentication.LoginPassword(
      data.url,
      data.login,
      data.password
    )

    def addAuthentication(data: MiniclustAuthenticationData): Unit = MiniclustAuthentication += coreObject(data)

    def removeAuthentication(data: MiniclustAuthenticationData): Unit = MiniclustAuthentication -= coreObject(data)

    def loginAuthentications(u: Unit): Seq[MiniclustAuthenticationData] = MiniclustAuthentication().flatMap:
      case lp: MiniclustAuthentication.LoginPassword =>
        Some:
          MiniclustAuthenticationData(
            lp.login,
            lp.password,
            lp.url
          )
      case _ => None

    def testAuthentication(data: MiniclustAuthenticationData): Seq[Test] =
      Seq(
        MiniclustAuthentication.test(coreObject(data)) match 
          case Success(_) => Test.passed()
          case Failure(f) => Test.error("failed", ErrorData(f))
      )



