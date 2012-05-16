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

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget._
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TextField
import scala.swing.event.ButtonClicked

class GliteEnvironmentPanelUI(pud: GliteEnvironmentDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IEnvironmentPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val voTextField = new TextField(20) {
    tooltip = Help.tooltip(i18n.getString("vo"),
      i18n.getString("voEx"))
  }
  val vomsTextField = new TextField(20) {
    tooltip = Help.tooltip(i18n.getString("voms"),
      i18n.getString("vomsEx"))
  }
  val bdiiTextField = new TextField(20) {
    tooltip = Help.tooltip(i18n.getString("bdii"),
      i18n.getString("bdiiEx"))
  }

  val proxyCheckBox = new CheckBox("MyProxy") { tooltip = Help.tooltip(i18n.getString("myProxy")) }
  val proxyURLTextField = new TextField(18) {
    tooltip = Help.tooltip(i18n.getString("proxyURL"),
      i18n.getString("proxyURLEx"))
  }
  val proxyURLLabel = new Label("url")

  val requirementCheckBox = new CheckBox("Requirements") { tooltip = Help.tooltip(i18n.getString("requirement")) }
  val architectureCheckBox = new CheckBox("64 bits") { tooltip = Help.tooltip(i18n.getString("64bits")) }

  val runtimeMemoryLabel = new Label("RuntimeMemory")
  val runtimeMemoryTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("runtimeMemory"),
      i18n.getString("runtimeMemoryEx"))
  }
  val workerNodeMemoryLabel = new Label("Worker memory")
  val workerNodeMemoryTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("workerNodeMemory"),
      i18n.getString("workerNodeMemoryEx"))
  }
  val maxCPUTimeLabel = new Label("Max CPU Time")
  val maxCPUTimeTextField = new TextField(4) {
    tooltip = Help.tooltip(i18n.getString("maxCPUTime"),
      i18n.getString("maxCPUTimeEx"))
  }
  val otherRequirementLabel = new Label("Other")
  val otherRequirementTextField = new TextField(16) { tooltip = Help.tooltip(i18n.getString("other")) }

  contents += (new PluginPanel("wrap 2") {
    contents += (new Label("VO"), "gap para")
    contents += voTextField
    contents += (new Label("VOMS"), "gap para")
    contents += vomsTextField
    contents += (new Label("BDII"), "gap para")
    contents += bdiiTextField
  }, "wrap")
  contents += (new PluginPanel("wrap 2") {
    contents += (proxyCheckBox, "wrap")
    contents += (proxyURLLabel, "gap para")
    contents += proxyURLTextField
  }, "wrap")
  contents += (new PluginPanel("wrap 2") {
    contents += (requirementCheckBox, "wrap")
    contents += (architectureCheckBox, "wrap")
    contents += (runtimeMemoryLabel, "gap para")
    contents += runtimeMemoryTextField
    contents += (workerNodeMemoryLabel, "gap para")
    contents += workerNodeMemoryTextField
    contents += (maxCPUTimeLabel, "gap para")
    contents += maxCPUTimeTextField
    contents += otherRequirementLabel
    contents += otherRequirementTextField
  }, "wrap")

  voTextField.text = pud.vo
  vomsTextField.text = pud.voms
  bdiiTextField.text = pud.bdii
  proxyURLTextField.text = pud.proxyURL
  proxyCheckBox.selected = pud.proxy
  requirementCheckBox.selected = pud.requirement
  architectureCheckBox.selected = pud.architecture64
  runtimeMemoryTextField.text = pud.runtimeMemory
  workerNodeMemoryTextField.text = pud.workerNodeMemory
  maxCPUTimeTextField.text = pud.maxCPUTime
  showProxy(pud.proxy)
  showRequirements(pud.requirement)
  otherRequirementTextField.text = pud.otherRequirements

  listenTo(`proxyCheckBox`, `requirementCheckBox`)
  reactions += {
    case ButtonClicked(`requirementCheckBox`) ⇒ showRequirements(requirementCheckBox.selected)
    case ButtonClicked(`proxyCheckBox`) ⇒ showProxy(proxyCheckBox.selected)
  }

  private def showProxy(b: Boolean) = {
    List(proxyURLLabel, proxyURLLabel, proxyURLTextField).foreach {
      _.visible = b
    }
  }

  private def showRequirements(b: Boolean) = {
    List(architectureCheckBox, runtimeMemoryLabel,
      runtimeMemoryTextField, workerNodeMemoryLabel,
      workerNodeMemoryTextField, maxCPUTimeLabel,
      maxCPUTimeTextField, otherRequirementLabel, otherRequirementTextField).foreach { _.visible = b }
  }

  override def saveContent(name: String) =
    new GliteEnvironmentDataUI(name,
      voTextField.text,
      vomsTextField.text,
      bdiiTextField.text,
      proxyCheckBox.selected,
      proxyURLTextField.text,
      requirementCheckBox.selected,
      architectureCheckBox.selected,
      runtimeMemoryTextField.text,
      workerNodeMemoryTextField.text,
      maxCPUTimeTextField.text,
      otherRequirementTextField.text)

}
