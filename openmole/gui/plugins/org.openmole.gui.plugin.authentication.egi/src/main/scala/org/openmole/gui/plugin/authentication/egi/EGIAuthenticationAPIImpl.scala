/**
 * Created by Romain Reuillon on 28/11/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.gui.plugin.authentication.egi

import org.openmole.core.preference.PreferenceLocation
import org.openmole.gui.ext.data._
import org.openmole.plugin.environment.egi._
import org.openmole.gui.ext.tool.server
import org.openmole.core.services._

import scala.util.{ Failure, Success, Try }

object EGIAuthenticationAPIImpl {
  val voTest = PreferenceLocation[Seq[String]]("AuthenicationPanel", "voTest", Some(Seq[String]()))
}

class EGIAuthenticationAPIImpl(s: Services) extends EGIAuthenticationAPI {

  implicit val services = s
  import services._

  private def authenticationFile(p: String) = {
    def path = p.replace(EGIAuthenticationData.authenticationDirectory, server.Utils.authenticationKeysFile.getAbsolutePath)
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
        Seq(EGIAuthenticationData(
          cypher.decrypt(p12.cypheredPassword),
          Some(p12.certificate.getPath)
        ))
      case x: Any ⇒ Seq()
    }

  def addAuthentication(data: EGIAuthenticationData): Unit =
    coreObject(data).foreach { a ⇒
      EGIAuthentication.update(a, test = false)
    }

  def removeAuthentication = EGIAuthentication.clear

  // To be used for ssh private key
  def deleteAuthenticationKey(keyName: String): Unit = authenticationFile(keyName).delete

  def testAuthentication(data: EGIAuthenticationData): Seq[Test] = {

    def testPassword(data: EGIAuthenticationData, test: EGIAuthentication ⇒ Try[Boolean]): Test = coreObject(data).map { d ⇒
      test(d) match {
        case Success(_) ⇒ Test.passed()
        case Failure(f) ⇒ Test.error("Invalid Password", ErrorData(f))
      }
    }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown " + data.name)))

    def test(data: EGIAuthenticationData, voName: String, test: (EGIAuthentication, String) ⇒ Try[Boolean]): Test = coreObject(data).map { d ⇒
      test(d, voName) match {
        case Success(_) ⇒ Test.passed(voName)
        case Failure(f) ⇒ Test.error("Invalid Password", ErrorData(f))
      }
    }.getOrElse(Test.error("Unknown error", MessageErrorData("Unknown " + data.name)))

    val vos = services.preference(EGIAuthenticationAPIImpl.voTest)

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

  override def setVOTest(vos: Seq[String]): Unit =
    services.preference.setPreference(EGIAuthenticationAPIImpl.voTest, vos)

  override def geVOTest(): Seq[String] =
    services.preference(EGIAuthenticationAPIImpl.voTest)

}
