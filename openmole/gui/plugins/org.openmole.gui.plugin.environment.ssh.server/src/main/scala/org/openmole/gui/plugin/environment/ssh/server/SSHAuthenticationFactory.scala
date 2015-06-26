package org.openmole.gui.plugin.environment.ssh.server

import java.io.File

import org.openmole.core.workspace.{Authentication, Workspace}
import org.openmole.gui.ext.data.AuthenticationData.{PrivateKeyAuthenticationData, LoginPasswordAuthenticationData}
import org.openmole.gui.ext.data.{AuthenticationData, AuthenticationFactory}
import org.openmole.gui.server.core.Utils._
import org.openmole.plugin.environment.ssh.{SSHAuthentication, LoginPassword, PrivateKey}

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

class SSHAuthenticationFactory extends AuthenticationFactory {

  implicit lazy val authProvider = Workspace.authenticationProvider

  def buildAuthentication(data: AuthenticationData) = {
    val auth = data match {
      case lp: LoginPasswordAuthenticationData => LoginPassword(lp.login, lp.cypheredPassword, lp.target)
      case key: PrivateKeyAuthenticationData => PrivateKey(new File(key.privateKey), key.login, key.cypheredPassword, key.target)
    }

    SSHAuthentication += auth
  }

  def allAuthenticationData: Seq[AuthenticationData] = {
    Workspace.authenticationProvider(classOf[SSHAuthentication]).map {
      _._1 match {
        case lp: LoginPassword => LoginPasswordAuthenticationData(lp.login, lp.cypheredPassword, lp.target)
        case key: PrivateKey => PrivateKeyAuthenticationData(key.privateKey.getCanonicalPath, key.login, key.cypheredPassword, key.target)
      }
    }
  }

}
