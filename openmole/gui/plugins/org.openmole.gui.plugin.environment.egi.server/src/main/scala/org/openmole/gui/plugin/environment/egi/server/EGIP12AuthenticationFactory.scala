package org.openmole.gui.plugin.environment.egi.server

import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.server.core.Utils._
import org.openmole.plugin.environment.egi.{EGIAuthentication, P12Certificate}

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

class EGIP12AuthenticationFactory extends AuthenticationFactory {

  implicit lazy val authProvider = Workspace.authenticationProvider

  def buildAuthentication(data: AuthenticationData) = {
    val auth = coreObject(data)
    auth.map { a => EGIAuthentication.update(a) }
  }

  def allAuthenticationData: Seq[AuthenticationData] = {
    EGIAuthentication() match {
      case Some(p12: P12Certificate) =>
        Seq(EGIP12AuthenticationData(
          Workspace.encrypt(p12.cypheredPassword),
          Some(p12.certificate)))
      case x: Any => Seq()
    }
  }



  def coreObject(data: AuthenticationData): Option[P12Certificate] = data match {
    case p12: EGIP12AuthenticationData => Some(P12Certificate(
      Workspace.decrypt(p12.cypheredPassword),
      p12.certificatePath.getOrElse(SafePath.empty): SafePath))
    case _ => None
  }

  def removeAuthentication(data: AuthenticationData) = EGIAuthentication.clear
}
