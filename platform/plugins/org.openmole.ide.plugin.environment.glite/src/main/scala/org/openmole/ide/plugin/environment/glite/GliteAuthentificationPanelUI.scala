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

package org.openmole.ide.plugin.environment.glite

import java.awt.Color
import javax.swing.BorderFactory
import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.misc.widget.ChooseFileTextField
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.environment.glite.GliteEnvironment
import scala.swing.ButtonGroup
import scala.swing.FileChooser.SelectionMode._
import scala.swing.Label
import scala.swing.PasswordField
import scala.swing.RadioButton
import scala.swing.Separator
import scala.swing.event.ButtonClicked
import scala.swing.event.SelectionChanged

class GliteAuthentificationPanelUI extends MigPanel("") with IAuthentificationPanelUI{
  val pem1TextField= new ChooseFileTextField("Certification path", "Select afile", Some("pem files"), FilesOnly,Some("pem")){
    text= Workspace.preference(GliteEnvironment.CertificatePathLocation)}
  val pem2TextField= new ChooseFileTextField("Key Path", "Select a file", Some("pem files"), FilesOnly,Some("pem")){
    text= Workspace.preference(GliteEnvironment.KeyPathLocation)}
  val p12TextField= new ChooseFileTextField("Certification path", "Select a file", Some("p12 file"), FilesOnly,Some("p12")){
    text= Workspace.preference(GliteEnvironment.P12CertificateLocation)}
  val proxyTextField = new ChooseFileTextField("nada", "Select a file", Some("proxy file"), FilesOnly,Some("proxy")){
    text= Workspace.preference(GliteEnvironment.ProxyLocation)}
  val password= new PasswordField(12)
  //{text= Workspace.preference(GliteEnvironment.PasswordLocation)}
  val pemButton = new RadioButton("pem")
  val p12Button = new RadioButton("p12")
  val proxyButton = new RadioButton("proxy")
  val groupButton = new ButtonGroup(pemButton,p12Button,proxyButton)
  contents+= new MigPanel("wrap"){
    contents+= pemButton
    contents+= p12Button
    contents+= proxyButton}
  contents+= buildPemPanel
  groupButton.select(pemButton)
  
  listenTo(pemButton,p12Button,proxyButton)
  reactions += {
    case ButtonClicked(`pemButton`) =>  
      contents.remove(1)
      contents+= buildPemPanel
      repaint
      revalidate
    case ButtonClicked(`p12Button`) =>  
      contents.remove(1)
      contents+= buildP12Panel
      repaint
      revalidate
    case ButtonClicked(`proxyButton`) =>  
      contents.remove(1)
      contents+= buildProxyPanel
      repaint
      revalidate
  }
  
  override def saveContent = println("TO be implemented the savecontent of GliteAuth")
  
  def buildPemPanel = new MigPanel("fillx,wrap 2","[left][grow,fill]","") {
    contents+= (new Label("Certification"),"gap para")
    contents+= pem1TextField
    contents+= (new Label("Key"),"gap para")
    contents+= pem2TextField
    contents+= (new Label("Password"),"gap para")
    contents+= password
    border= BorderFactory.createLineBorder(new Color(102,102,102),1)
  }
  
  def buildP12Panel = new MigPanel("fillx,wrap 2","[left][grow,fill]","") {
    contents+= (new Label("Certification"),"gap para")
    contents+= p12TextField
    contents+= (new Label("password"),"gap para")
    contents+= password
    border= BorderFactory.createLineBorder(new Color(102,102,102),1)
  }
  
  def buildProxyPanel = new MigPanel("fillx,wrap 2","[left][grow,fill]","") {
    contents+= (new Label("Proxy"),"gap para")
    contents+= proxyTextField
    border= BorderFactory.createLineBorder(new Color(102,102,102),1)
  }
}
