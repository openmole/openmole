/*
 * Copyright (C) 2011 Romain Reuillon
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

import fr.iscpif.gridscale.ssh.SSHUserPasswordAuthentication
import org.openmole.core.batch.authentication.CypheredPassword

object LoginPassword {

  def apply(
    login: String,
    cypheredPassword: String,
    target: String) = new LoginPassword(login, cypheredPassword, target)

}

class LoginPassword(
    val login: String,
    val cypheredPassword: String,
    val target: String) extends SSHAuthentication with CypheredPassword { a ⇒

  def apply = new SSHUserPasswordAuthentication {
    val user = a.login
    val password = a.password
  }

  override def toString = super.toString + ", Login / password, login = " + login

}
