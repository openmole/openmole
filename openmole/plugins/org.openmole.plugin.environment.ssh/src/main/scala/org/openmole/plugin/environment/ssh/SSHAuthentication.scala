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

import java.util.UUID

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workspace.{ Workspace, AuthenticationProvider }

object SSHAuthentication {

  def address(login: String, host: String, port: Int) = s"$login@$host:$port"

  def apply()(implicit authentications: AuthenticationProvider) = authentications(classOf[SSHAuthentication])

  def apply(target: String)(implicit authentications: AuthenticationProvider): SSHAuthentication = {
    val list = authentications(classOf[SSHAuthentication])
    val auth = list.reverse.find { e ⇒ target.matches(e.regexp) }
    auth.getOrElse(throw new UserBadDataError("No authentication method found for " + target))
  }

  def apply(login: String, host: String, port: Int = 22)(implicit authentications: AuthenticationProvider): SSHAuthentication =
    apply(address(login, host, port))(authentications)

  def +=(a: SSHAuthentication) =
    Workspace.authentications.save[SSHAuthentication](a, eq)

  def -=(a: SSHAuthentication) =
    Workspace.authentications.remove[SSHAuthentication](a, eq)

  def clear() = Workspace.authentications.clear[SSHAuthentication]

  private def eq(a1: SSHAuthentication, a2: SSHAuthentication) = (a1.login, a1.target) == (a2.login, a2.target)
}

trait SSHAuthentication {
  def target: String
  def login: String
  def regexp = ".*" + login + "@" + target + ".*"

  def apply(implicit authenticationProvider: AuthenticationProvider): fr.iscpif.gridscale.ssh.SSHAuthentication

  override def toString = "Target = " + target
}
