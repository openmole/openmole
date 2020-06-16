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
package org.openmole.gui.plugin.authentication.sshkey

import org.openmole.core.services._
import org.openmole.gui.ext.data.{ ErrorData, Test }
import org.openmole.gui.ext.server.utils
import org.openmole.plugin.environment.ssh._

import scala.util._

class PrivateKeyAuthenticationApiImpl(s: Services) extends PrivateKeyAuthenticationAPI {

  import s._

  private def authenticationFile(key: String) = new java.io.File(utils.authenticationKeysFile, key)

  private def coreObject(data: PrivateKeyAuthenticationData) =
    data.privateKey match {
      case Some(pk: String) ⇒ Some(PrivateKey(
        authenticationFile(pk),
        data.login,
        cypher.encrypt(data.cypheredPassword),
        data.target,
        data.port.toInt
      ))
      case _ ⇒ None
    }

  def privateKeyAuthentications(): Seq[PrivateKeyAuthenticationData] = SSHAuthentication().flatMap {
    _ match {
      case key: PrivateKey ⇒ Seq(PrivateKeyAuthenticationData(
        Some(key.privateKey.getName),
        key.login,
        cypher.decrypt(key.cypheredPassword),
        key.host,
        key.port.toString
      ))
      case _ ⇒ None
    }
  }

  def addAuthentication(data: PrivateKeyAuthenticationData): Unit = coreObject(data).foreach { co ⇒
    SSHAuthentication += co
  }

  def removeAuthentication(data: PrivateKeyAuthenticationData): Unit = coreObject(data).foreach { co ⇒
    SSHAuthentication -= co
  }

  def testAuthentication(data: PrivateKeyAuthenticationData): Seq[Test] = Seq(
    coreObject(data).map { co ⇒
      SSHAuthentication.test(co) match {
        case Success(_) ⇒ Test.passed()
        case Failure(f) ⇒ Test.error("failed", ErrorData(f))
      }
    }
  ).flatten

}