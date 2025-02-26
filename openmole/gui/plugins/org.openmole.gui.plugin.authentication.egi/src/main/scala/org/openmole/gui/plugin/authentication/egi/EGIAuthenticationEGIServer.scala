package org.openmole.gui.plugin.authentication.egi

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

import endpoints4s.http4s.server
import cats.effect.IO
import org.http4s.HttpRoutes
import org.openmole.core.preference.PreferenceLocation
import org.openmole.core.services.Services
import org.openmole.plugin.environment.egi._
import scala.util.{Try, Success, Failure}
import org.openmole.gui.shared.data.*
import org.openmole.gui.server.ext.utils
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.tool.file.*

object EGIAuthenticationAPIServer:
  val voTest = PreferenceLocation[Seq[String]]("AuthenticationPanel", "voTest", Some(Seq[String]()))

class EGIAuthenticationEGIServer(s: Services)
  extends APIServer
  with EGIAuthenticationAPI {

  implicit val services: Services = s
  import services._

  val egiAuthenticationsRoute =
    egiAuthentications.errorImplementedBy(_ => impl.egiAuthentications())

  val addAuthenticationRoute =
    addAuthentication.errorImplementedBy(a => impl.addAuthentication(a))

  val removeAuthenticationsRoute =
    removeAuthentications.errorImplementedBy(impl.removeAuthentications)

  val setVOTestsRoute =
    setVOTests.errorImplementedBy(vo => impl.setVOTest(vo))

  val getVOTestsRoute =
    getVOTests.errorImplementedBy(_ => impl.geVOTests())

  val testAuthenticationRoute =
    testAuthentication.errorImplementedBy(d => impl.testAuthentication(d))

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(egiAuthenticationsRoute, addAuthenticationRoute, removeAuthenticationsRoute, setVOTestsRoute, getVOTestsRoute, testAuthenticationRoute)
  )

  object impl {
    private def coreObject(data: EGIAuthenticationData) =
      data.privateKey.map { pk => P12Certificate(data.password, safePathToFile(pk)) }

    def egiAuthentications(): Seq[EGIAuthenticationData] =
      EGIAuthentication() match
        case Some(p12: P12Certificate) =>
          Seq(
            EGIAuthenticationData(
              p12.password,
              Some(p12.certificate.toSafePath(using ServerFileSystemContext.Authentication))
            )
          )
        case _ => Seq()

    def addAuthentication(data: EGIAuthenticationData): Unit =
      coreObject(data).foreach: a =>
        EGIAuthentication.update(a, test = false)

    def removeAuthentications(data: EGIAuthenticationData, removeFile: Boolean) =
      EGIAuthentication.clear
      if removeFile then safePathToFile(EGIAuthenticationData.directory).recursiveDelete

    def testAuthentication(data: EGIAuthenticationData): Seq[Test] =

      def testPassword(data: EGIAuthenticationData, test: EGIAuthentication => Try[Boolean]): Test = coreObject(data).map { d =>
        test(d) match
          case Success(_) => Test.passed()
          case Failure(f) => Test.error("Invalid Password", ErrorData(f))
      }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown error", None)))

      def test(data: EGIAuthenticationData, voName: String, test: (EGIAuthentication, String) => Try[Boolean]): Test = coreObject(data).map { d =>
        test(d, voName) match
          case Success(_) => Test.passed(voName)
          case Failure(f) => Test.error("Invalid Password", ErrorData(f))
      }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown error", None)))

      val vos = services.preference(EGIAuthenticationAPIServer.voTest)

      def aggregate(message: String, password: Test, proxy: Test, dirac: Test): Test =
        val all = Seq(password, proxy, dirac)
        val error = all.flatMap(_.error).headOption

        error match
          case Some(e) => Test.error("failed", e)
          case _ => Test.passed(message)

      vos.map { voName =>
        Try {
          aggregate(
            voName,
            testPassword(data, EGIAuthentication.testPassword(_)),
            test(data, voName, EGIAuthentication.testProxy(_, _)),
            test(data, voName, EGIAuthentication.testDIRACAccess(_, _))
          )
        } match {
          case Success(a) => a
          case Failure(f) => Test.error("Error", ErrorData(f))
        }
      }

    def setVOTest(vos: Seq[String]): Unit =
      services.preference.setPreference(EGIAuthenticationAPIServer.voTest, vos)

    def geVOTests(): Seq[String] =
      services.preference(EGIAuthenticationAPIServer.voTest)

  }
}
