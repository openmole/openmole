/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TextField
import scala.swing.event.ButtonClicked

class GliteEnvironmentPanelUI(pud: GliteEnvironmentDataUI) extends MigPanel("fillx","[left][grow,fill]","") with IEnvironmentPanelUI{
  
  val voTextField = new TextField(20) {tooltip = Help.tooltip("Virtual Organization name","vo.complex-systems.eu")}
  val vomsTextField = new TextField(20) {tooltip = Help.tooltip("Virtual Organization Membership Service url","voms://voms.grid.auth.gr:15160/C=GR/O=HellasGrid/OU=auth.gr/CN=voms.grid.auth.gr")}
  val bdiiTextField = new TextField(20) {tooltip = Help.tooltip("Berkeley Database Information Index url","ldap://topbdii.grif.fr:2170")}
  
  val proxyCheckBox = new CheckBox("MyProxy") {tooltip = Help.tooltip("Set Proxy settings")}
  val proxyURLTextField = new TextField(18) {tooltip = Help.tooltip("","")} 
  val proxyUserTextField = new TextField(18){tooltip = Help.tooltip("","")}
  val proxyURLLabel = new Label("url")
  val proxyUserLabel = new Label("user")
  
  val requirementCheckBox = new CheckBox("Requirements") {tooltip = Help.tooltip("Adds specific requirements")}
  val architectureCheckBox = new CheckBox("64 bits") {tooltip = Help.tooltip("64 bits worker nodes only")}
  val runtimeMemoryLabel = new Label("Runtime memory") 
  val runtimeMemoryTextField = new TextField(4) 
  val workerNodeMemoryLabel = new Label("Worker memory")
  val workerNodeMemoryTextField = new TextField(4) {tooltip = Help.tooltip("Specify a minimal size of RAM for the worker nodes","4Go")}
  val maxCPUTimeLabel = new Label("Max CPU Time")
  val maxCPUTimeTextField = new TextField(4)
  val otherRequirementLabel = new Label("Other")
  val otherRequirementTextField = new TextField(16) {tooltip = Help.tooltip("Free requirement specification","4Go")}
  
  contents+= (new MigPanel("wrap 2") {
      contents+= (new Label("VO"),"gap para")
      contents+= voTextField
      contents+= (new Label("VOMS"),"gap para")
      contents+= vomsTextField
      contents+= (new Label("BDII"),"gap para")
      contents+= bdiiTextField
    },"wrap")
  contents+= (new MigPanel("wrap 2") {
      contents+= (proxyCheckBox,"wrap")
      contents+= (proxyURLLabel,"gap para")
      contents+= proxyURLTextField
      contents+= (proxyUserLabel,"gap para")
      contents+= proxyUserTextField
    },"wrap")
  contents+= (new MigPanel("wrap 2") {
      contents+= (requirementCheckBox,"wrap")
      contents+= (architectureCheckBox,"wrap")
      contents+= (runtimeMemoryLabel,"gap para")
      contents+= runtimeMemoryTextField
      contents+= (workerNodeMemoryLabel,"gap para")
      contents+= workerNodeMemoryTextField
      contents+= (maxCPUTimeLabel,"gap para")
      contents+= maxCPUTimeTextField
      contents+= otherRequirementLabel
      contents+= otherRequirementTextField
    },"wrap")
  
  voTextField.text = pud.vo
  vomsTextField.text = pud.voms
  bdiiTextField.text = pud.bdii
  proxyURLTextField.text = pud.proxyURL
  proxyUserTextField.text = pud.proxyUser
  proxyCheckBox.selected = pud.proxy
  requirementCheckBox.selected = pud.requirement
  architectureCheckBox.selected = pud.architecture64
  runtimeMemoryTextField.text = pud.runtimeMemory
  workerNodeMemoryTextField.text = pud.workerNodeMemory
  maxCPUTimeTextField.text = pud.maxCPUTime
  showProxy(pud.proxy)
  showRequirements(pud.requirement)
  otherRequirementTextField.text = pud.otherRequirements
  
  listenTo(`proxyCheckBox`,`requirementCheckBox`)
  reactions += {
    case ButtonClicked(`requirementCheckBox`) => showRequirements(requirementCheckBox.selected)
    case ButtonClicked(`proxyCheckBox`) => showProxy(proxyCheckBox.selected)}
      
  private def showProxy(b: Boolean) = {
    List(proxyURLLabel, proxyURLLabel, proxyUserLabel, proxyURLTextField, proxyUserTextField).foreach{
      _.visible = b
    }
  }
    
  private def showRequirements(b: Boolean) = {
    List(architectureCheckBox, runtimeMemoryLabel, 
         runtimeMemoryTextField, workerNodeMemoryLabel,
         workerNodeMemoryTextField, maxCPUTimeLabel,
         maxCPUTimeTextField,otherRequirementLabel,otherRequirementTextField).foreach{_.visible = b}
  }
  
  override def saveContent(name: String) = 
    new GliteEnvironmentDataUI(name,
                               voTextField.text,
                               vomsTextField.text,
                               bdiiTextField.text,
                               proxyCheckBox.selected,
                               proxyURLTextField.text,
                               proxyUserTextField.text,
                               requirementCheckBox.selected,
                               architectureCheckBox.selected,
                               runtimeMemoryTextField.text,
                               workerNodeMemoryTextField.text,
                               maxCPUTimeTextField.text,
                               otherRequirementTextField.text)
  
}
