/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */

package org.openmole.plugin.environment.ssh

import java.io.File

import org.openmole.core.exception._
import org.openmole.core.authentication._
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.tool.crypto.Cypher
import io.circe.generic.auto.*
import org.openmole.core.json.given
import scala.util.Try

object SSHAuthentication:
  given isGridScaleAuthentication(using cypher: Cypher): _root_.gridscale.ssh.SSHAuthentication[org.openmole.plugin.environment.ssh.SSHAuthentication] with
    override def login(a: org.openmole.plugin.environment.ssh.SSHAuthentication) =
      a match
        case a: LoginPassword ⇒ a.login
        case a: PrivateKey    ⇒ a.login

    override def authenticate(a: org.openmole.plugin.environment.ssh.SSHAuthentication, sshClient: _root_.gridscale.ssh.sshj.SSHClient) =
      a match
        case a: LoginPassword =>
          val gsAuth = gridscale.authentication.UserPassword(a.login, cypher.decrypt(a.cypheredPassword))
          implicitly[gridscale.ssh.SSHAuthentication[gridscale.authentication.UserPassword]].authenticate(gsAuth, sshClient)
        case a: PrivateKey =>
          val gsAuth = gridscale.authentication.PrivateKey(a.privateKey, cypher.decrypt(a.cypheredPassword), a.login)
          implicitly[gridscale.ssh.SSHAuthentication[gridscale.authentication.PrivateKey]].authenticate(gsAuth, sshClient)


  def apply()(using AuthenticationStore): Seq[SSHAuthentication] =
    Authentication.load[SSHAuthentication]

  def find(login: String, host: String, port: Int = 22)(using AuthenticationStore): SSHAuthentication =
    val list = apply()
    val auth = list.findLast(a => (a.login, a.host, a.port) == (login, host, port))
    auth.getOrElse(throw new UserBadDataError(s"No authentication method found for $login@$host:$port"))

  def +=(a: SSHAuthentication)(using AuthenticationStore) =
    Authentication.save[SSHAuthentication](a, eq)

  def -=(a: SSHAuthentication)(implicit authenticationStore: AuthenticationStore) =
    Authentication.remove[SSHAuthentication](a, eq)

  def clear()(implicit authenticationStore: AuthenticationStore) = Authentication.clear[SSHAuthentication]

  private def eq(a1: SSHAuthentication, a2: SSHAuthentication) = (a1.getClass, a1.login, a1.host, a1.port) == (a2.getClass, a2.login, a2.host, a2.port)

  def test(a: SSHAuthentication)(implicit cypher: Cypher, authenticationStore: AuthenticationStore, preference: Preference) =
    implicit val intp = gridscale.ssh.SSH()
    try
      Try {
        val server = gridscale.ssh.SSHServer(a.host, a.port, preference(SSHEnvironment.timeOut))(a)
        gridscale.ssh.home(server)
      }.map(_ ⇒ true)
    finally intp().close()


sealed trait SSHAuthentication:
  def host: String
  def port: Int
  def login: String

case class LoginPassword(
  login:            String,
  cypheredPassword: String,
  host:             String,
  port:             Int    = 22
) extends SSHAuthentication:
  override def toString = s"$login@$host:$port using password"

case class PrivateKey(
  privateKey:       File,
  login:            String,
  cypheredPassword: String,
  host:             String,
  port:             Int    = 22
) extends SSHAuthentication:
  override def toString = s"$login@$host:$port using private key $privateKey"


