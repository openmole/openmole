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

import java.awt.Dimension
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.core.batch.authentication.HostAuthenticationMethod
import org.openmole.core.batch.authentication.LoginPassword
import org.openmole.core.batch.authentication.PrivateKey
import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow._
import org.openmole.misc.workspace.Workspace
import scala.swing.Action
import scala.swing.FileChooser.SelectionMode._
import scala.swing.Label
import scala.swing.MyComboBox
import scala.swing.Panel
import scala.swing.PasswordField
import scala.swing.TextField
import scala.swing.event.Key._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.event.SelectionChanged

object SSHAuthentificationPanelUI {
  trait ConnectionMethod
  case class Login extends ConnectionMethod
  case class SSHKey extends ConnectionMethod

  class SSHAuthentificationPanel(var data: SSHAuthentificationData)
      extends PluginPanel("") with IPanel[SSHAuthentificationData] {

    val popupPanel = new PopupSSHAuthentificationPanel(data)
    val linkLabel = new LinkLabel(
      expandName,
      new Action("") { def apply = displayPopup },
      4,
      "73a5d2",
      false)

    contents += linkLabel

    def displayPopup: Unit = {
      if (DialogDisplayer.getDefault.notify(
        new DialogDescriptor(popupPanel.peer,
          "SSH Authentitfications settings")).equals(NotifyDescriptor.OK_OPTION)) {
        data = popupPanel.content
        linkLabel.link(expandName)

        revalidate
        repaint
      }
    }

    def expandName = {
      if (data.connectionData.login == "" && data.host == "") "new"
      else data.connectionData.login + "@" + data.host
    }

    def content = popupPanel.content

  }

  class SSHAuthentificationData(val host: String = "",
                                val connectionData: ConnectionData = new ConnectionData) extends IData

  class ConnectionData(val connectionMethod: ConnectionMethod = new Login,
                       val login: String = "",
                       val password: String = "",
                       val target: String = "",
                       val pritvateKey: String = "",
                       val publicKey: String = "")

  class SSHAuthentificationFactory extends IFactory[SSHAuthentificationData] {
    def apply = new SSHAuthentificationPanel(new SSHAuthentificationData)
  }

  class PopupSSHAuthentificationPanel(data: SSHAuthentificationData) extends PluginPanel("wrap") {

    var loginPasswordPanel = new LoginPasswordPanel(data.connectionData)
    var sshKeyPanel = new SSHKeyPanel(data.connectionData)

    val authentificationTypeComboBox = new MyComboBox(List(loginPasswordPanel,
      sshKeyPanel))
    data.connectionData.connectionMethod match {
      case x: Login ⇒ authentificationTypeComboBox.selection.index = 0
      case x: SSHKey ⇒ authentificationTypeComboBox.selection.index = 1
      case _ ⇒
    }

    listenTo(`authentificationTypeComboBox`)
    authentificationTypeComboBox.selection.reactions += {
      case SelectionChanged(`authentificationTypeComboBox`) ⇒
        displayAuthentification
    }

    val hostTextField = new TextField("zebulon.iscpif.fr", 15)

    contents += new PluginPanel("") {
      contents += new Label("zebulon.iscpif.fr")
      contents += hostTextField
    }
    contents += authentificationTypeComboBox
    displayAuthentification
    preferredSize = new Dimension(300, 280)

    def displayAuthentification = {
      if (contents.size > 2) contents.remove(2)
      contents += authentificationTypeComboBox.selection.item
      revalidate
      repaint
    }

    trait AuthentificationPanel extends Panel {
      def content: ConnectionData
    }

    class LoginPasswordPanel(data: ConnectionData) extends PluginPanel("wrap 2") with AuthentificationPanel {
      override def toString = "Login / Password"
      val loginTextField = new TextField(data.login, 15)
      val passwordTextField = new PasswordField(data.password, 15)
      val targetTextField = new TextField(data.target, 15)

      contents += new Label("Login")
      contents += loginTextField
      contents += new Label("Password")
      contents += passwordTextField
      contents += new Label("Target")
      contents += targetTextField

      def content = new ConnectionData(new Login,
        loginTextField.text,
        new String(passwordTextField.password),
        targetTextField.text)
    }

    class SSHKeyPanel(data: ConnectionData) extends LoginPasswordPanel(data) {
      override def toString = "SSH Key"

      val privateKeyTextField = new TextField(data.pritvateKey, 15)
      val publicKeyTextField = new TextField(data.publicKey, 15)

      contents += new Label("Private key")
      contents += privateKeyTextField
      contents += new Label("Public key")
      contents += publicKeyTextField

      override def content = new ConnectionData(new SSHKey,
        loginTextField.text,
        new String(passwordTextField.password),
        targetTextField.text,
        privateKeyTextField.text,
        publicKeyTextField.text)
    }

    def content = new SSHAuthentificationData(hostTextField.text,
      authentificationTypeComboBox.selection.item.content)
  }
}

import SSHAuthentificationPanelUI._
class SSHAuthentificationPanelUI extends PluginPanel("") with IAuthentificationPanelUI {

  val panelList =
    Workspace.persistentList(classOf[HostAuthenticationMethod]).map { hm ⇒
      hm match {
        case (i: Int, x: LoginPassword) ⇒
          new SSHAuthentificationPanel(new SSHAuthentificationData("zebulon.iscpif.fr",
            new ConnectionData(new Login,
              x.login,
              Workspace.decrypt(x.cypheredPassword),
              x.target)))
        case (i: Int, x: PrivateKey) ⇒
          new SSHAuthentificationPanel(new SSHAuthentificationData("zebulon.iscpif.fr",
            new ConnectionData(new SSHKey,
              x.login,
              Workspace.decrypt(x.cypheredPassword),
              x.target,
              x.privateKeyPath,
              x.publicKeyPath)))
      }
    }.toList

  val multiPanel = new MultiPanel("SSH Authentifications",
    new SSHAuthentificationFactory,
    panelList,
    CLOSE_IF_EMPTY,
    ADD)
  contents += multiPanel.panel

  def saveContent = {
    var i = 0
    for (j ← 0 to Workspace.persistentList(classOf[HostAuthenticationMethod]).size)
      Workspace.persistentList(classOf[HostAuthenticationMethod]) -= j
    multiPanel.content.foreach { data ⇒
      data.connectionData.connectionMethod match {
        case x: Login ⇒ Workspace.persistentList(classOf[HostAuthenticationMethod])(i) =
          new LoginPassword(data.connectionData.login,
            Workspace.encrypt(new String(data.connectionData.password)),
            data.connectionData.target)
        case x: SSHKey ⇒ Workspace.persistentList(classOf[HostAuthenticationMethod])(i) =
          new PrivateKey(data.connectionData.pritvateKey,
            data.connectionData.publicKey,
            data.connectionData.login,
            Workspace.encrypt(new String(data.connectionData.password)),
            data.connectionData.target)
      }
      i += 1
    }

  }

}