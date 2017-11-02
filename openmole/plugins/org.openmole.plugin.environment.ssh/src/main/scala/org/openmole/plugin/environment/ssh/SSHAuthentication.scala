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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workspace._
import org.openmole.plugin.environment.batch.authentication.CypheredPassword
import org.openmole.core.authentication._
import org.openmole.core.serializer.SerializerService
import org.openmole.tool.crypto.Cypher

import scala.util.Try

object SSHAuthentication {

  def apply()(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    Authentication.allByCategory.getOrElse(classOf[SSHAuthentication].getName, Seq.empty).map(_.asInstanceOf[SSHAuthentication])

  def find(login: String, host: String, port: Int = 22)(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService): SSHAuthentication = {
    val list = apply()
    val auth = list.reverse.find { a ⇒ (a.login, a.host, a.port) == (login, host, port) }
    auth.getOrElse(throw new UserBadDataError(s"No authentication method found for $login@$host:$port"))
  }

  def +=(a: SSHAuthentication)(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    Authentication.save[SSHAuthentication](a, eq)

  def -=(a: SSHAuthentication)(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    Authentication.remove[SSHAuthentication](a, eq)

  def clear()(implicit authenticationStore: AuthenticationStore) = Authentication.clear[SSHAuthentication]

  private def eq(a1: SSHAuthentication, a2: SSHAuthentication) = (a1.getClass, a1.login, a1.host, a1.port) == (a2.getClass, a2.login, a2.host, a2.port)

  def test(a: SSHAuthentication)(implicit cypher: Cypher, authenticationStore: AuthenticationStore, serializerService: SerializerService) = {
    Try(fr.iscpif.gridscale.ssh.SSHJobService(a.host, a.port)(a.apply).home).map(_ ⇒ true)
  }
}

trait SSHAuthentication {
  def host: String
  def port: Int
  def login: String
  def apply(implicit cypher: Cypher): fr.iscpif.gridscale.ssh.SSHAuthentication
  override def toString = s"$login@$host:$port"
}

case class LoginPassword(
  val login:            String,
  val cypheredPassword: String,
  val host:             String,
  val port:             Int    = 22
) extends SSHAuthentication with CypheredPassword { a ⇒

  def apply(implicit cypher: Cypher) =
    fr.iscpif.gridscale.authentication.UserPassword(a.login, a.password)

  override def toString = super.toString + ", Login / password, login = " + login
}

case class PrivateKey(
  val privateKey:       File,
  val login:            String,
  val cypheredPassword: String,
  val host:             String,
  val port:             Int    = 22
) extends SSHAuthentication with CypheredPassword { a ⇒

  override def apply(implicit cypher: Cypher) =
    fr.iscpif.gridscale.authentication.PrivateKey(a.login, a.privateKey, a.password)

  override def toString =
    super.toString +
      ", PrivateKey = " + privateKey +
      ", Login = " + login

}

