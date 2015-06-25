package org.openmole.gui.plugin.environment.ssh.server

import org.openmole.core.workspace.Workspace
import org.openmole.core.workspace.Workspace._
import org.openmole.gui.ext.data.AuthenticationFactory
import org.openmole.gui.plugin.environment.ssh.ext.{PrivateKeyAuthenticationData, LoginPasswordAuthenticationData, SSHAuthenticationData}
import org.openmole.gui.server.core.Utils._
import org.openmole.plugin.environment.ssh.{LoginPassword, PrivateKey}

/*
 * Copyright (C) 25/06/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class SSHAuthenticationFactory(val data: SSHAuthenticationData) extends AuthenticationFactory {

  def buildAuthentication = {

    val auth = data match {
      case lp: LoginPasswordAuthenticationData => LoginPassword(lp.login, lp.cypheredPassword, lp.target)
      case key: PrivateKeyAuthenticationData => PrivateKey(key.privateKey, key.login, key.cypheredPassword, key.target)
    }

    val authProvider = Workspace.authenticationProvider

  //  SSHAuthentication += auth
  }
}
