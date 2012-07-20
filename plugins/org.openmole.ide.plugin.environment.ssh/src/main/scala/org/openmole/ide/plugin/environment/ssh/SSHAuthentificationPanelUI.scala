/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.environment.ssh

import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow._
import org.openmole.misc.workspace.Workspace
import scala.swing.ButtonGroup
import scala.swing.FileChooser.SelectionMode._
import scala.swing.Label
import scala.swing.PasswordField
import scala.swing.RadioButton
import scala.swing.TextField
import scala.swing.event.Key._

class SSHAuthentificationPanelUI extends PluginPanel("", "[left][right]", "") with IAuthentificationPanelUI {
  var passString = ""
  var initButton: Option[RadioButton] = None
  val loginPasswordButton = new RadioButton("login / password")
  val sshKeyButton = new RadioButton("SSH key")
  val groupButton = new ButtonGroup {
    buttons += loginPasswordButton
    buttons += sshKeyButton
  }

  val multiPanel = new MultiPanel("SSH Authentification",
    Factory,
    List())
  //List(new LoginPasswordPanel(new LoginPasswordData("aaa", "b")),
  //  new LoginPasswordPanel(new LoginPasswordData("bb", "cc"))))
  contents += multiPanel.panel

  def saveContent = {}

  object Factory extends IFactory[LoginPasswordData] {
    def apply = new LoginPasswordPanel(new LoginPasswordData)
  }

  class LoginPasswordPanel(data: LoginPasswordData) extends PluginPanel("wrap 2") with IPanel[LoginPasswordData] {
    val loginTextField = new TextField(data.login, 15)
    val passwordTextField = new PasswordField(data.password, 15)

    contents += new Label("Login")
    contents += loginTextField
    contents += new Label("Password")
    contents += passwordTextField

    def content = new LoginPasswordData(loginTextField.text, passwordTextField.text)
  }

  class LoginPasswordData(val login: String = "",
                          val password: String = "") extends IData
}