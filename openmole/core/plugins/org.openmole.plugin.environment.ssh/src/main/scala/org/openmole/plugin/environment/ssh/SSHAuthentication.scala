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

import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace.{ AuthenticationProvider, Workspace }

object SSHAuthentication {

  def apply()(implicit authentications: AuthenticationProvider) = authentications(classOf[SSHAuthentication])
  def update(i: Int, a: SSHAuthentication) = Workspace.setAuthentication(i, a)
  def apply(i: Int)(implicit authentications: AuthenticationProvider) = authentications(classOf[SSHAuthentication])(i)
  def apply(target: String, authentications: AuthenticationProvider) = {
    val list = authentications(classOf[SSHAuthentication])
    list.find { e ⇒ target.matches(e.regexp) }.getOrElse(throw new UserBadDataError("No authentication method found for " + target))
  }

  def apply(login: String, host: String, port: Int, authentications: AuthenticationProvider): SSHAuthentication = apply(address(login, host, port), authentications)
  def address(login: String, host: String, port: Int) = s"$login@$host:$port"

}

trait SSHAuthentication {
  def target: String
  def login: String
  def regexp = ".*" + login + "@" + target + ".*"

  def apply(): fr.iscpif.gridscale.ssh.SSHAuthentication

  override def toString = "Target = " + target
}
