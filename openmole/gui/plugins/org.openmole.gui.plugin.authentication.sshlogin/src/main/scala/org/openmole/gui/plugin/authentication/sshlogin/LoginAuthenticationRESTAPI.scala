package org.openmole.gui.plugin.authentication.sshlogin

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
import org.openmole.plugin.environment.ssh.*

import util.{Failure, Success, Try}
import org.openmole.gui.server.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*


object LoginAuthenticationRESTAPI:
  val loginAuthentications: TapirEndpoint[Unit, Seq[LoginAuthenticationData]] =
    endpoint.get.in("ssh" / "login-authentications").out(jsonBody[Seq[LoginAuthenticationData]]).errorOut(jsonBody[ErrorData])

  val addAuthentication: TapirEndpoint[LoginAuthenticationData, Unit] =
    endpoint.post.in("ssh" / "add-login-authentication").in(jsonBody[LoginAuthenticationData]).errorOut(jsonBody[ErrorData])

  val removeAuthentication: TapirEndpoint[LoginAuthenticationData, Unit] =
    endpoint.post.in("ssh" / "remove-login-authentication").in(jsonBody[LoginAuthenticationData]).errorOut(jsonBody[ErrorData])

  val testAuthentication: TapirEndpoint[LoginAuthenticationData, Seq[Test]] =
    endpoint.post.in("ssh" / "test-login-authentication").in(jsonBody[LoginAuthenticationData]).out(jsonBody[Seq[Test]]).errorOut(jsonBody[ErrorData])


class LoginAuthenticationServer(s: Services):
  
  val routes: HttpRoutes[IO] = 
    import LoginAuthenticationRESTAPI.*
    routesFromEndpoints(
      loginAuthentications.implementedBy(impl.loginAuthentications),
      addAuthentication.implementedBy(impl.addAuthentication),
      removeAuthentication.implementedBy(impl.removeAuthentication),
      testAuthentication.implementedBy(impl.testAuthentication)
    )

  object impl:

    import s._

    private def coreObject(data: LoginAuthenticationData) = LoginPassword(
      data.login,
      data.password,
      data.target,
      data.port.toInt
    )

    def addAuthentication(data: LoginAuthenticationData): Unit = SSHAuthentication += coreObject(data)

    def removeAuthentication(data: LoginAuthenticationData): Unit = SSHAuthentication -= coreObject(data)

    def loginAuthentications(u: Unit): Seq[LoginAuthenticationData] = SSHAuthentication().flatMap:
      case lp: LoginPassword =>
        Some(LoginAuthenticationData(
          lp.login,
          lp.password,
          lp.host,
          lp.port.toString
        ))
      case _ => None

    def testAuthentication(data: LoginAuthenticationData): Seq[Test] =
      Seq(
        SSHAuthentication.test(coreObject(data)) match {
          case Success(_) => Test.passed()
          case Failure(f) => Test.error("failed", ErrorData(f))
        }
      )



