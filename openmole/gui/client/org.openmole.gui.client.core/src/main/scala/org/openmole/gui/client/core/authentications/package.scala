package org.openmole.gui.client.core

import org.openmole.gui.ext.data.{ PrivateKeyAuthenticationData, AuthenticationData, LoginPasswordAuthenticationData, EGIP12AuthenticationData }
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.ext.dataui.PanelWithID

/*
 * Copyright (C) 19/02/16 // mathieu.leclaire@openmole.org
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

package object authentications {
  def panelUI(data: AuthenticationData): PanelUI = data match {
    case egi: EGIP12AuthenticationData                  ⇒ new EGIP12AuthenticationPanel(egi)
    case loginPassword: LoginPasswordAuthenticationData ⇒ new SSHLoginPasswordAuthenticationPanel(loginPassword)
    case privateKey: PrivateKeyAuthenticationData       ⇒ new SSHPrivateKeyAuthenticationPanel(privateKey)
  }

  private def naming(data: AuthenticationData) = data match {
    case e: EGIP12AuthenticationData         ⇒ "EGI certificate"
    case lp: LoginPasswordAuthenticationData ⇒ "SSH login/password"
    case _                                   ⇒ "SSH key"
  }

  private def emptyData(data: AuthenticationData) = data match {
    case e: EGIP12AuthenticationData         ⇒ EGIP12AuthenticationData()
    case lp: LoginPasswordAuthenticationData ⇒ LoginPasswordAuthenticationData()
    case _                                   ⇒ PrivateKeyAuthenticationData()
  }

  case class AuthPanelWithID(data: AuthenticationData, name: String) extends PanelWithID {
    type DATA = AuthenticationData

    def panel: PanelUI = panelUI(data)

    def emptyClone: AuthPanelWithID = copy(data = emptyData(data))
  }

  def panelWithID(authData: AuthenticationData): AuthPanelWithID = AuthPanelWithID(authData, naming(authData))

  def factories: Seq[AuthPanelWithID] = Seq(
    authentications.panelWithID(LoginPasswordAuthenticationData()),
    authentications.panelWithID(PrivateKeyAuthenticationData()),
    authentications.panelWithID(EGIP12AuthenticationData())
  )

}
