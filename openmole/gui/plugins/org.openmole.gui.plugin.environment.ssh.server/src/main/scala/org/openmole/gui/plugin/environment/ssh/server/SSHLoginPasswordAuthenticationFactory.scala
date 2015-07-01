package org.openmole.gui.plugin.environment.ssh.server

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data.AuthenticationData.LoginPasswordAuthenticationData
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

class SSHLoginPasswordAuthenticationFactory extends AuthenticationFactory {

  implicit lazy val authProvider = Workspace.authenticationProvider

  def buildAuthentication(data: AuthenticationData) = {
    val auth = data match {
      case lp: LoginPasswordAuthenticationData => Some(LoginPassword(lp.login, lp.cypheredPassword, lp.target))
      case _=> None
    }

    auth.map{a=> SSHAuthentication += a}
  }

  def allAuthenticationData: Seq[AuthenticationData] = {
    println("ALLL AuTH " + Workspace.authenticationProvider(classOf[SSHAuthentication]).size)
    Workspace.authenticationProvider(classOf[SSHAuthentication]).flatMap {
      e=> println("find " + e)
      e match {
        case lp: LoginPassword => Some(LoginPasswordAuthenticationData(lp.login, lp.cypheredPassword, lp.target))
        case _=> None
      }
    }
  }

}
