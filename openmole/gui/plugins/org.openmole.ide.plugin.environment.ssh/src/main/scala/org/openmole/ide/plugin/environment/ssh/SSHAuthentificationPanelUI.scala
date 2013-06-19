/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

import java.io.File
import java.awt.Dimension
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow._
import org.openmole.misc.workspace.Workspace
import scala.swing.Action
import scala.swing.Label
import scala.swing.MyComboBox
import scala.swing.Panel
import scala.swing.PasswordField
import scala.swing.TextField
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import scala.swing.event.SelectionChanged
import org.openmole.plugin.environment.ssh.{ SSHAuthentication, PrivateKey, LoginPassword }

object SSHAuthentificationPanelUI {
  sealed trait ConnectionMethod
  case object Login extends ConnectionMethod
  case object SSHKey extends ConnectionMethod

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
      if (data.connectionData.login == "" && data.connectionData.target == "") "new"
      else data.connectionData.login + "@" + data.connectionData.target
    }

    def content = popupPanel.content

  }

  class SSHAuthentificationData(val connectionData: ConnectionData = new ConnectionData) extends IData

  class ConnectionData(val connectionMethod: ConnectionMethod = Login,
                       val login: String = "",
                       val password: String = "",
                       val target: String = "",
                       val pritvateKey: String = "")

  class SSHAuthentificationFactory extends IFactory[SSHAuthentificationData] {
    def apply = new SSHAuthentificationPanel(new SSHAuthentificationData)
  }

  class PopupSSHAuthentificationPanel(data: SSHAuthentificationData) extends PluginPanel("wrap") {

    var loginPasswordPanel = new LoginPasswordPanel(data.connectionData)
    var sshKeyPanel = new SSHKeyPanel(data.connectionData)

    val authentificationTypeComboBox = new MyComboBox(List(loginPasswordPanel,
      sshKeyPanel))
    data.connectionData.connectionMethod match {
      case Login  ⇒ authentificationTypeComboBox.selection.index = 0
      case SSHKey ⇒ authentificationTypeComboBox.selection.index = 1
      case _      ⇒
    }

    listenTo(`authentificationTypeComboBox`)
    authentificationTypeComboBox.selection.reactions += {
      case SelectionChanged(`authentificationTypeComboBox`) ⇒
        displayAuthentification
    }

    contents += authentificationTypeComboBox
    displayAuthentification
    preferredSize = new Dimension(300, 280)

    def displayAuthentification = {
      if (contents.size > 1) contents.remove(1)
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

      contents += new Label("Target")
      contents += targetTextField
      contents += new Label("Login")
      contents += loginTextField
      contents += new Label("Password")
      contents += passwordTextField

      def content = new ConnectionData(Login,
        loginTextField.text,
        new String(passwordTextField.password),
        targetTextField.text)
    }

    class SSHKeyPanel(data: ConnectionData) extends LoginPasswordPanel(data) {
      override def toString = "SSH Key"

      val privateKeyTextField = new TextField(data.pritvateKey, 15)

      contents += new Label("Private key")
      contents += privateKeyTextField

      override def content = new ConnectionData(SSHKey,
        loginTextField.text,
        new String(passwordTextField.password),
        targetTextField.text,
        privateKeyTextField.text)
    }

    def content = new SSHAuthentificationData(authentificationTypeComboBox.selection.item.content)
  }
}

import SSHAuthentificationPanelUI._
class SSHAuthentificationPanelUI extends PluginPanel("") with IAuthentificationPanelUI {

  override val components = List()
  val panelList =
    Workspace.persistentList(classOf[SSHAuthentication]).map { hm ⇒
      hm match {
        case (i: Int, x: LoginPassword) ⇒
          new SSHAuthentificationPanel(new SSHAuthentificationData(
            new ConnectionData(Login,
              x.login,
              Workspace.decrypt(x.cypheredPassword),
              x.target)))
        case (i: Int, x: PrivateKey) ⇒
          new SSHAuthentificationPanel(new SSHAuthentificationData(
            new ConnectionData(SSHKey,
              x.login,
              Workspace.decrypt(x.cypheredPassword),
              x.target,
              x.privateKey.getPath)))
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
    for (j ← 0 to Workspace.persistentList(classOf[SSHAuthentication]).size)
      Workspace.persistentList(classOf[SSHAuthentication]) -= j
    multiPanel.content.foreach { data ⇒
      data.connectionData.connectionMethod match {
        case Login ⇒ Workspace.persistentList(classOf[SSHAuthentication])(i) =
          new LoginPassword(data.connectionData.login,
            Workspace.encrypt(new String(data.connectionData.password)),
            data.connectionData.target)
        case SSHKey ⇒ Workspace.persistentList(classOf[SSHAuthentication])(i) =
          new PrivateKey(new File(data.connectionData.pritvateKey),
            data.connectionData.login,
            Workspace.encrypt(new String(data.connectionData.password)),
            data.connectionData.target)
      }
      i += 1
    }
  }

}