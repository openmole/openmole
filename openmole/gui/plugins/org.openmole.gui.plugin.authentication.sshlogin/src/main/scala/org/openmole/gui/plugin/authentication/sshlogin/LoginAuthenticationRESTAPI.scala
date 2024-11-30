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

trait LoginAuthenticationRESTAPI extends RESTAPI:
  val loginAuthentications: ErrorEndpoint[Unit, Seq[LoginAuthenticationData]] =
    errorEndpoint(get(path / "ssh" / "login-authentications"), ok(jsonResponse[Seq[LoginAuthenticationData]]))

  val addAuthentication: ErrorEndpoint[LoginAuthenticationData, Unit] =
    errorEndpoint(post(path / "ssh" / "add-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Unit]))

  val removeAuthentication: ErrorEndpoint[LoginAuthenticationData, Unit] =
    errorEndpoint(post(path / "ssh" / "remove-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Unit]))

  val testAuthentication: ErrorEndpoint[LoginAuthenticationData, Seq[Test]] =
    errorEndpoint(post(path / "ssh" / "test-login-authentication", jsonRequest[LoginAuthenticationData]), ok(jsonResponse[Seq[Test]]))


class LoginAuthenticationServer(s: Services) extends APIServer with LoginAuthenticationRESTAPI:

  val loginAuthenticationsRoute =
    loginAuthentications.errorImplementedBy { _ => impl.loginAuthentications() }

  val addAuthenticationRoute =
    addAuthentication.errorImplementedBy { a => impl.addAuthentication(a) }

  val removeAuthenticationRoute =
    removeAuthentication.errorImplementedBy { a => impl.removeAuthentication(a) }

  val testAuthenticationRoute =
    testAuthentication.errorImplementedBy { a => impl.testAuthentication(a) }

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(loginAuthenticationsRoute, addAuthenticationRoute, removeAuthenticationRoute, testAuthenticationRoute)
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

    def loginAuthentications(): Seq[LoginAuthenticationData] = SSHAuthentication().flatMap:
      case lp: LoginPassword ⇒
        Some(LoginAuthenticationData(
          lp.login,
          lp.password,
          lp.host,
          lp.port.toString
        ))
      case _ ⇒ None

    def testAuthentication(data: LoginAuthenticationData): Seq[Test] =
      Seq(
        SSHAuthentication.test(coreObject(data)) match {
          case Success(_) ⇒ Test.passed()
          case Failure(f) ⇒ Test.error("failed", ErrorData(f))
        }
      )



