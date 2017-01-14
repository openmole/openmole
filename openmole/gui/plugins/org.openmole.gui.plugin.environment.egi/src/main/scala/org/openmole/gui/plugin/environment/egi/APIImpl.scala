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

import java.nio.file._

import org.openmole.gui.ext.data._
import org.openmole.gui.plugin.environment.egi.AuthenticationFactories.EGIP12Factory

class APIImpl extends API {

  def addAuthentication(data: AuthenticationData): Unit = AuthenticationFactories.addAuthentication(data)

  def egiAuthentications(): Seq[EGIAuthenticationData] = EGIP12Factory.allAuthenticationData

  def removeAuthentication(data: AuthenticationData) = AuthenticationFactories.removeAuthentication(data)

  def deleteAuthenticationKey(keyName: String): Unit = Utils.authenticationFile(keyName).delete

  def renameKey(keyName: String, newName: String): Unit =
    Files.move(Utils.authenticationFile(keyName).toPath, Utils.authenticationFile(newName).toPath, StandardCopyOption.REPLACE_EXISTING)

  def testAuthentication(data: AuthenticationData, vos: Seq[String] = Seq()): Seq[AuthenticationTest] =
    data match {
      case d: EGIAuthenticationData ⇒
        if (vos.isEmpty) Seq(EGIAuthenticationTest("empty VO", AuthenticationTest.empty, AuthenticationTest.empty, AuthenticationTest.empty))
        else AuthenticationFactories.testEGIAuthentication(d, vos)
      case lp: LoginPasswordAuthenticationData ⇒ AuthenticationFactories.testLoginPasswordSSHAuthentication(lp)
      case pk: PrivateKeyAuthenticationData    ⇒ AuthenticationFactories.testPrivateKeySSHAuthentication(pk)
      case _                                   ⇒ Seq(AuthenticationTestBase(false, Error("Cannot test this authentication")))
    }

}
