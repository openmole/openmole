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

package org.openmole.ide.plugin.environment.glite

import java.awt.Color
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.glite._
import scala.swing.ButtonGroup
import scala.swing.FileChooser.SelectionMode._
import scala.swing.Label
import scala.swing.PasswordField
import scala.swing.RadioButton
import scala.swing.event.ButtonClicked
import org.openmole.misc.workspace.Workspace
import scala.swing.event.Key._
import java.io.File
import swing.event.ButtonClicked
import scala.Some

object GliteAuthentificationPanelUI {
  val RED = new Color(212, 0, 0)
  val GREEN = new Color(136, 170, 0)

  lazy val vomses = {
    val x = xml.XML.loadFile(GliteAuthentication.voCards)
    val cards = (x \ "IDCard")
    val names = cards map (_.attribute("Name").get.text)
    val urls = cards map (c ⇒ (c \ "EnrollmentUrl").head.text)
    names zip urls
  }

}

class GliteAuthentificationPanelUI extends PluginPanel("", "[left][right]", "") with IAuthentificationPanelUI {
  var passString = ""
  var initButton: Option[RadioButton] = None
  val pemButton = new RadioButton("pem")
  val p12Button = new RadioButton("p12")
  val proxyButton = new RadioButton("proxy")
  val groupButton = new ButtonGroup {
    buttons += pemButton
    buttons += p12Button
    buttons += proxyButton
  }

  val pem1TextField = new ChooseFileTextField("Certificate path", "Select afile", Some("pem files"), FilesOnly, Some("pem"))
  val pem2TextField = new ChooseFileTextField("Key Path", "Select a file", Some("pem files"), FilesOnly, Some("pem"))
  val p12TextField = new ChooseFileTextField("Certificate path", "Select a file", Some("p12 file"), FilesOnly, Some("p12"))
  val proxyTextField = new ChooseFileTextField("", "Select a file", Some("proxy file"), FilesOnly, Some("proxy"))

  val pemPanel = buildPemPanel
  val p12Panel = buildP12Panel
  val proxyPanel = buildProxyPanel

  val pem = (pemButton, pemPanel, { pem1TextField.text = ""; pem2TextField.text = "" })
  val p12 = (p12Button, p12Panel, { p12TextField.text = "" })
  val proxy = (proxyButton, proxyPanel, { proxyTextField.text = "" })

  val pemPassField = Some(new PasswordField(passString, 20))
  val p12PassField = Some(new PasswordField(passString, 20))

  addButtons
  try {
    Workspace.persistentList(classOf[GliteAuthentication]).headOption match {
      case Some((i: Int, x: P12Certificate)) ⇒
        initButton = Some(p12Button)
        passString = Workspace.decrypt(x.cypheredPassword)
        p12TextField.text = x.certificate.getAbsolutePath
        addP12
      case Some((i: Int, x: PEMCertificate)) ⇒
        initButton = Some(pemButton)
        pem1TextField.text = x.certificate.getAbsolutePath
        pem2TextField.text = x.key.getAbsolutePath
        passString = Workspace.decrypt(x.cypheredPassword)
        addPem
      case Some((i: Int, x: ProxyFile)) ⇒
        initButton = Some(proxyButton)
        proxyTextField.text = x.proxy.getAbsolutePath
        addProxy
      case _ ⇒
        initButton = Some(p12Button)
        addP12
    }

    pemPanel.contents += pemPassField.get
    p12Panel.contents += p12PassField.get

    listenTo(`pemButton`, `p12Button`, `proxyButton`)
    reactions += {
      case ButtonClicked(`pemButton`) ⇒ addPem
      case ButtonClicked(`p12Button`) ⇒ addP12
      case ButtonClicked(`proxyButton`) ⇒ addProxy
    }

    groupButton.select(initButton.get)

  } catch { case e: Throwable ⇒ StatusBar().block(e.getMessage, stack = e.getStackTraceString) }

  def addButtons =
    contents += new PluginPanel("wrap", "", "[]15[]15[]") {
      contents += pemButton
      contents += p12Button
      contents += proxyButton
    }

  def addPem = {
    clean
    contents += pem._2
    refresh
  }
  def addP12 = {
    clean
    contents += p12._2
    refresh
  }
  def addProxy = {
    clean
    contents += proxy._2
    refresh
  }

  def clean = if (contents.size == 2) contents.remove(1)

  def refresh = {
    repaint
    revalidate
  }

  def saveContent =
    try {
      pemPassField match {
        case Some(x: PasswordField) ⇒
          if (pemButton.selected) Workspace.persistentList(classOf[GliteAuthentication])(0) =
            new PEMCertificate(Workspace.encrypt(new String(x.password)),
              new File(pem1TextField.text),
              new File(pem2TextField.text))
          else if (p12Button.selected)
            Workspace.persistentList(classOf[GliteAuthentication])(0) =
              new P12Certificate(Workspace.encrypt(new String(p12PassField.get.password)),
                new File(p12TextField.text))
          else if (proxyButton.selected) {
            Workspace.persistentList(classOf[GliteAuthentication])(0) =
              new ProxyFile(new File(proxyTextField.text))
          }
      }
      (pem :: p12 :: proxy :: Nil).filterNot(_._1.selected).foreach(_._3)
    } catch { case e: Throwable ⇒ StatusBar().block(e.getMessage, stack = e.getStackTraceString) }

  def buildPemPanel =
    new PluginPanel("fillx,wrap 2", "[left][grow,fill]", "[top]") {
      contents += (new Label("Certification"), "gap para")
      contents += pem1TextField
      contents += (new Label("Key"), "gap para")
      contents += pem2TextField
      contents += (new Label("Password"), "gap para")
    }

  def buildP12Panel =
    new PluginPanel("fillx,wrap 2", "[left][grow,fill]", "[top]") {
      contents += (new Label("Certification"), "gap para")
      contents += p12TextField
      contents += (new Label("Password"), "gap para")
    }

  def buildProxyPanel =
    new PluginPanel("fillx,wrap 2", "[left][grow,fill]", "[top]") {
      contents += (new Label("Proxy"), "gap para")
      contents += proxyTextField
    }
}
