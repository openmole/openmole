package org.openmole.gui.client.core
//
//import org.openmole.gui.ext.data.{ PanelUI, _ }
//import fr.iscpif.scaladget.api.BootstrapTags.Displayable

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
//
//package object authentications {
//  def panelUI(data: AuthenticationData): PanelUI = data match {
//    case egi: EGIP12AuthenticationPluginData                  ⇒ new EGIP12AuthenticationPanel(egi)
//    case loginPassword: LoginPasswordAuthenticationPluginData ⇒ new SSHLoginPasswordAuthenticationPanel(loginPassword)
//    case privateKey: PrivateKeyAuthenticationPluginData       ⇒ new SSHPrivateKeyAuthenticationPanel(privateKey)
//  }
//
//  private def naming(data: AuthenticationData) = data match {
//    case e: EGIP12AuthenticationPluginData         ⇒ "EGI P12 certificate"
//    case lp: LoginPasswordAuthenticationPluginData ⇒ "SSH login/password"
//    case _                                         ⇒ "SSH private key"
//  }
//
//  private def emptyData(data: AuthenticationData) = data match {
//    case e: EGIP12AuthenticationPluginData         ⇒ EGIP12AuthenticationPluginData()
//    case lp: LoginPasswordAuthenticationPluginData ⇒ LoginPasswordAuthenticationPluginData()
//    case _                                         ⇒ PrivateKeyAuthenticationPluginData()
//  }
//
//  case class AuthPanelWithID(
//      data:                AuthenticationData,
//      name:                String,
//      authenticationTests: Seq[AuthenticationTest] = Seq(AuthenticationTestBase(false, Error("Authentication not tested yet")))
//
//  ) extends Displayable {
//    type DATA = AuthenticationData
//
//    def panel: PanelUI = panelUI(data)
//
//    def emptyClone: AuthPanelWithID = copy(data = emptyData(data))
//  }
//
//  def panelWithID(authData: AuthenticationData): AuthPanelWithID = AuthPanelWithID(authData, naming(authData))
//
//  def factories: Seq[AuthPanelWithID] = Seq(
//    authentications.panelWithID(LoginPasswordAuthenticationPluginData()),
//    authentications.panelWithID(PrivateKeyAuthenticationPluginData()),
//    authentications.panelWithID(EGIP12AuthenticationPluginData())
//  )
//
//}
