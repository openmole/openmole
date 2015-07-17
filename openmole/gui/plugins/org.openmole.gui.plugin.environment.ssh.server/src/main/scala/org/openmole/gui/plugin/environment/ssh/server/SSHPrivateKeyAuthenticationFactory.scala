package org.openmole.gui.plugin.environment.ssh.server

/*
 * Copyright (C) 01/07/15 // mathieu.leclaire@openmole.org
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

import java.io.File
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data.{SafePath, PrivateKeyAuthenticationData, AuthenticationFactory, AuthenticationData}
import org.openmole.gui.server.core.Utils._
import org.openmole.plugin.environment.ssh.{SSHAuthentication, PrivateKey}

class SSHPrivateKeyAuthenticationFactory extends AuthenticationFactory {

  implicit def authProvider = Workspace.authenticationProvider

  def buildAuthentication(data: AuthenticationData) = {
    val auth = coreObject(data)
    auth.map { a => SSHAuthentication += a }
  }

  def allAuthenticationData: Seq[AuthenticationData] = SSHAuthentication().flatMap {
    _ match {
      case key: PrivateKey => Some(PrivateKeyAuthenticationData(
        Some(key.privateKey.getName),
        key.login,
        Workspace.decrypt(key.cypheredPassword),
        key.target))
      case _ => None
    }
  }

  def coreObject(data: AuthenticationData): Option[PrivateKey] = data match {
    case keyData: PrivateKeyAuthenticationData =>
      keyData.privateKey match {
        case Some(pk: String) => Some(PrivateKey(authenticationFile(pk),
          keyData.login,
          Workspace.encrypt(keyData.cypheredPassword),
          keyData.target))
        case _ => None
      }
    case _ => None
  }

  def removeAuthentication(data: AuthenticationData) = coreObject(data).map { e => SSHAuthentication -= e }
}
