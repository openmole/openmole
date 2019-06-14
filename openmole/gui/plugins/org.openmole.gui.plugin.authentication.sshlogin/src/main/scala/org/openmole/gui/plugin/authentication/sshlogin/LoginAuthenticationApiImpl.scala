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
package org.openmole.gui.plugin.authentication.sshlogin

import org.openmole.core.services._
import org.openmole.gui.ext.data._
import org.openmole.plugin.environment.ssh.{ LoginPassword, SSHAuthentication }

import scala.util._

class LoginAuthenticationApiImpl(s: Services) extends LoginAuthenticationAPI {

  import s._

  private def coreObject(data: LoginAuthenticationData) = LoginPassword(
    data.login,
    cypher.encrypt(data.password),
    data.target,
    data.port.toInt
  )

  def addAuthentication(data: LoginAuthenticationData): Unit = SSHAuthentication += coreObject(data)

  def removeAuthentication(data: LoginAuthenticationData): Unit = SSHAuthentication -= coreObject(data)

  def loginAuthentications(): Seq[LoginAuthenticationData] = SSHAuthentication().flatMap {
    _ match {
      case lp: LoginPassword ⇒
        Some(LoginAuthenticationData(
          lp.login,
          cypher.decrypt(lp.cypheredPassword),
          lp.host,
          lp.port.toString
        ))
      case _ ⇒ None
    }
  }

  def testAuthentication(data: LoginAuthenticationData): Seq[Test] =
    Seq(
      SSHAuthentication.test(coreObject(data)) match {
        case Success(_) ⇒ Test.passed()
        case Failure(f) ⇒ Test.error("failed", ErrorData(f))
      }
    )

}