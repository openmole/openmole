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
import org.openmole.gui.ext.server.utils
import org.openmole.plugin.environment.egi._
import scala.util.{Try, Success, Failure}
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.server.*

object EGIAuthenticationAPIServer {
  val voTest = PreferenceLocation[Seq[String]]("AuthenticationPanel", "voTest", Some(Seq[String]()))
}

class EGIAuthenticationEGIServer(s: Services)
  extends APIServer
  with EGIAuthenticationAPI {

  implicit val services: Services = s
  import services._

  val egiAuthenticationsRoute =
    egiAuthentications.implementedBy(_ => impl.egiAuthentications())

  val addAuthenticationRoute =
    addAuthentication.implementedBy(a => impl.addAuthentication(a))

  val removeAuthenticationsRoute =
    removeAuthentications.implementedBy(_ => impl.removeAuthentications())

  val setVOTestsRoute =
    setVOTests.implementedBy(vo => impl.setVOTest(vo))

  val getVOTestsRoute =
    getVOTests.implementedBy(_ => impl.geVOTests())

  val testAuthenticationRoute =
    testAuthentication.implementedBy(d => impl.testAuthentication(d))

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(egiAuthenticationsRoute, addAuthenticationRoute, removeAuthenticationsRoute, setVOTestsRoute, getVOTestsRoute, testAuthenticationRoute)
  )

  object impl {
    private def authenticationFile(p: String) = {
      def path = p.replace(EGIAuthenticationData.authenticationDirectory, utils.authenticationKeysFile.getAbsolutePath)

      new java.io.File(path)
    }

    private def coreObject(data: EGIAuthenticationData) = data.privateKey.map { pk ⇒
      P12Certificate(
        cypher.encrypt(data.cypheredPassword),
        authenticationFile(pk)
      )
    }

    def egiAuthentications(): Seq[EGIAuthenticationData] =
      EGIAuthentication() match {
        case Some(p12: P12Certificate) ⇒
          Seq(
            EGIAuthenticationData(
              cypher.decrypt(p12.cypheredPassword),
              Some(p12.certificate.getPath)
            )
          )
        case x: Any ⇒ Seq()
      }

    def addAuthentication(data: EGIAuthenticationData): Unit =
      coreObject(data).foreach { a ⇒
        EGIAuthentication.update(a, test = false)
      }

    def removeAuthentications() = EGIAuthentication.clear

    // To be used for ssh private key
    def deleteAuthenticationKey(keyName: String): Unit = authenticationFile(keyName).delete

    def testAuthentication(data: EGIAuthenticationData): Seq[Test] = {

      def testPassword(data: EGIAuthenticationData, test: EGIAuthentication ⇒ Try[Boolean]): Test = coreObject(data).map { d ⇒
        test(d) match {
          case Success(_) ⇒ Test.passed()
          case Failure(f) ⇒ Test.error("Invalid Password", ErrorData(f))
        }
      }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown " + data.name, None)))

      def test(data: EGIAuthenticationData, voName: String, test: (EGIAuthentication, String) ⇒ Try[Boolean]): Test = coreObject(data).map { d ⇒
        test(d, voName) match {
          case Success(_) ⇒ Test.passed(voName)
          case Failure(f) ⇒ Test.error("Invalid Password", ErrorData(f))
        }
      }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown " + data.name, None)))

      val vos = services.preference(EGIAuthenticationAPIServer.voTest)

      vos.map { voName ⇒
        Try {
          EGIAuthenticationTest(
            voName,
            testPassword(data, EGIAuthentication.testPassword(_)),
            test(data, voName, EGIAuthentication.testProxy(_, _)),
            test(data, voName, EGIAuthentication.testDIRACAccess(_, _))
          )
        } match {
          case Success(a) ⇒ a
          case Failure(f) ⇒ EGIAuthenticationTest("Error")
        }
      }
    }

    def setVOTest(vos: Seq[String]): Unit =
      services.preference.setPreference(EGIAuthenticationAPIServer.voTest, vos)

    def geVOTests(): Seq[String] =
      services.preference(EGIAuthenticationAPIServer.voTest)

  }
}
