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
import org.openmole.misc.workspace.Workspace

object SSHAuthentication {

  def apply() = Workspace.persistentList(classOf[SSHAuthentication])
  def update(i: Int, a: SSHAuthentication) = Workspace.persistentList(classOf[SSHAuthentication])(i) = a
  def apply(i: Int) = Workspace.persistentList(classOf[SSHAuthentication])(i)
  def apply(target: String) = {
    val list = Workspace.persistentList(classOf[SSHAuthentication])
    list.find { case (i, e) ⇒ target.matches(e.regexp) }.getOrElse(throw new UserBadDataError("No authentication method found for " + target))._2
  }

  def apply(login: String, host: String, port: Int): SSHAuthentication = apply(address(login, host, port))
  def address(login: String, host: String, port: Int) = s"$login@$host:$port"

}

trait SSHAuthentication {
  def target: String
  def login: String
  def regexp = ".*" + login + "@" + target + ".*"

  def apply(): fr.iscpif.gridscale.ssh.SSHAuthentication

  override def toString = "Target = " + target
}
