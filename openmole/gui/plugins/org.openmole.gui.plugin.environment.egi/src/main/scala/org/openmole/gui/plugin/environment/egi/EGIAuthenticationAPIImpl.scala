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
package org.openmole.gui.plugin.environment.egi

import java.nio.file.{ Files, StandardCopyOption }

import org.openmole.gui.ext.data._
import org.openmole.plugin.environment.egi.{ EGIAuthentication, P12Certificate }
import org.openmole.core.workspace.{ Decrypt, Workspace }
import org.openmole.gui.ext.plugin.server.Configurations

import scala.util.{ Failure, Success, Try }

class EGIAuthenticationAPIImpl extends EGIAuthenticationAPI {

  implicit def workspace: Workspace = Workspace.instance

  implicit def decrypt: Decrypt = Decrypt(workspace)

  private def coreObject(data: EGIAuthenticationData) = data.privateKey.map { pk ⇒
    P12Certificate(
      Workspace.encrypt(data.cypheredPassword),
      Utils.authenticationFile(pk)
    )
  }

  def egiAuthentications(): Seq[EGIAuthenticationData] = {
    EGIAuthentication() match {
      case Some(p12: P12Certificate) ⇒
        Seq(EGIAuthenticationData(
          Workspace.decrypt(p12.cypheredPassword),
          Some(p12.certificate.getName)
        ))
      case x: Any ⇒ Seq()
    }
  }

  def addAuthentication(data: EGIAuthenticationData): Unit = {
    coreObject(data).foreach { a ⇒
      EGIAuthentication.update(a, test = false)
    }
  }

  def removeAuthentication = EGIAuthentication.clear

  // To be used for ssh private key
  def deleteAuthenticationKey(keyName: String): Unit = Utils.authenticationFile(keyName).delete

  def renameKey(keyName: String, newName: String): Unit =
    Files.move(Utils.authenticationFile(keyName).toPath, Utils.authenticationFile(newName).toPath, StandardCopyOption.REPLACE_EXISTING)

  def testAuthentication(data: EGIAuthenticationData): Seq[EGIAuthenticationTest] = {

    def testPassword(data: EGIAuthenticationData, test: EGIAuthentication ⇒ Try[Boolean]): AuthenticationTest = coreObject(data).map { d ⇒
      test(d) match {
        case Success(_) ⇒ AuthenticationTest.passed
        case Failure(f) ⇒ AuthenticationTest.error("Invalid Password", false, ErrorBuilder(f))
      }
    }.getOrElse(AuthenticationTest.error("Unknown error", false, Error("Unknown " + data.name)))

    def test(data: EGIAuthenticationData, voName: String, test: (EGIAuthentication, String) ⇒ Try[Boolean]): AuthenticationTest = coreObject(data).map { d ⇒
      test(d, voName) match {
        case Success(_) ⇒ AuthenticationTest.passed
        case Failure(f) ⇒ AuthenticationTest.error("Invalid Password", false, ErrorBuilder(f))
      }
    }.getOrElse(AuthenticationTest.error("Unknown error", false, Error("Unknown " + data.name)))

    val vos = Configurations(VOTest).map { _.split(",").toSeq }.getOrElse(Seq())

    vos.map { voName ⇒
      Try {
        EGIAuthenticationTest(
          voName,
          testPassword(data, EGIAuthentication.testPassword),
          test(data, voName, EGIAuthentication.testProxy),
          test(data, voName, EGIAuthentication.testDIRACAccess)
        )
      } match {
        case Success(a) ⇒ a
        case Failure(f) ⇒ EGIAuthenticationTest("Error")
      }
    }

  }

}
