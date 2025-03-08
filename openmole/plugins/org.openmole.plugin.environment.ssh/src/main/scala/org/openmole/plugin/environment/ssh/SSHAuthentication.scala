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
import org.openmole.core.json.given
import scala.util.Try

import io.circe.*
import io.circe.generic.auto.*

object SSHAuthentication:

  given isGridScaleAuthentication: _root_.gridscale.ssh.SSHAuthentication[org.openmole.plugin.environment.ssh.SSHAuthentication] with
    override def login(a: org.openmole.plugin.environment.ssh.SSHAuthentication) =
      a match
        case a: LoginPassword => a.login
        case a: PrivateKey    => a.login

    override def authenticate(a: org.openmole.plugin.environment.ssh.SSHAuthentication, sshClient: _root_.gridscale.ssh.sshj.SSHClient) =
      a match
        case a: LoginPassword =>
          val gsAuth = gridscale.authentication.UserPassword(a.login, a.password)
          implicitly[gridscale.ssh.SSHAuthentication[gridscale.authentication.UserPassword]].authenticate(gsAuth, sshClient)
        case a: PrivateKey =>
          val gsAuth = gridscale.authentication.PrivateKey(a.privateKey, a.password, a.login)
          implicitly[gridscale.ssh.SSHAuthentication[gridscale.authentication.PrivateKey]].authenticate(gsAuth, sshClient)

  def apply()(using store: AuthenticationStore, cypher: Cypher): Seq[SSHAuthentication] =
    Authentication.load[SSHAuthentication].map:
      case l: LoginPassword => l.copy(cypheredPassword = cypher.decrypt(l.cypheredPassword))
      case k: PrivateKey => k.copy(cypheredPassword = cypher.decrypt(k.cypheredPassword))


  def find(login: String, host: String, port: Int = 22)(using AuthenticationStore, Cypher): SSHAuthentication =
    val list = apply()
    val auth = list.findLast(a => (a.login, a.host, a.port) == (login, host, port))
    auth.getOrElse(throw new UserBadDataError(s"No authentication method found for $login@$host:$port"))

  def +=(a: SSHAuthentication)(using store: AuthenticationStore, cypher: Cypher): Unit =
    val s =
      a match
        case l: LoginPassword => l.copy(cypheredPassword = cypher.encrypt(l.cypheredPassword))
        case k: PrivateKey => k.copy(cypheredPassword = cypher.encrypt(k.cypheredPassword))

    Authentication.save[SSHAuthentication](s, eq)

  def -=(a: SSHAuthentication)(implicit authenticationStore: AuthenticationStore): Unit =
    Authentication.remove[SSHAuthentication](a, eq)

  def clear()(implicit authenticationStore: AuthenticationStore) = Authentication.clear[SSHAuthentication]

  private def eq(a1: SSHAuthentication, a2: SSHAuthentication) = (a1.getClass, a1.login, a1.host, a1.port) == (a2.getClass, a2.login, a2.host, a2.port)

  def test(a: SSHAuthentication)(implicit cypher: Cypher, authenticationStore: AuthenticationStore, preference: Preference) =
    val server = gridscale.ssh.SSHServer(a.host, a.port, preference(SSHEnvironment.timeOut))(a)
    Try:
      gridscale.ssh.SSH.withSSH(server):
          gridscale.ssh.home()
        .map(_ => true)


sealed trait SSHAuthentication:
  def host: String
  def port: Int
  def login: String

object LoginPassword:
  def apply(
    login: String,
    password: String,
    host: String,
    port: Int = 22) = new LoginPassword(login, password, host, port)

case class LoginPassword(
  login:            String,
  private[ssh] val cypheredPassword: String,
  host:             String,
  port:             Int
) extends SSHAuthentication:
  override def toString = s"$login@$host:$port using password"
  def password = cypheredPassword

object PrivateKey:
  def apply(
    privateKey:       File,
    login:            String,
    password: String,
    host:             String,
    port:             Int    = 22) = new PrivateKey(privateKey, login, password, host, port)

case class PrivateKey(
  privateKey:       File,
  login:            String,
  private[ssh] val cypheredPassword: String,
  host:             String,
  port:             Int
) extends SSHAuthentication:
  override def toString = s"$login@$host:$port using private key $privateKey"
  def password = cypheredPassword


