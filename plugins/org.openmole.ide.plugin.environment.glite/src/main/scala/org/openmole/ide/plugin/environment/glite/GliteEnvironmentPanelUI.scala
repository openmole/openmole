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
import org.openmole.ide.plugins.misc.RequirementPanelUI
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget._
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

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

  val requirements = new RequirementPanelUI(pud.architecture64,
                                            pud.runtimeMemory,
                                            pud.workerNodeMemory,
                                            pud.maxCPUTime,
                                            pud.otherRequirements)
  
  val tabbedPane = new TabbedPane
  tabbedPane.pages += new TabbedPane.Page("Requirements",
                                          new PluginPanel("wrap 2") {
      contents += (new Label("VO"), "gap para")
      contents += voTextField
      contents += (new Label("VOMS"), "gap para")
      contents += vomsTextField
      contents += (new Label("BDII"), "gap para")
      contents += bdiiTextField
      contents += (proxyCheckBox, "wrap")
      contents += (proxyURLLabel, "gap para")
      contents += proxyURLTextField
    })

  tabbedPane.pages += requirements
  
  voTextField.text = pud.vo
  vomsTextField.text = pud.voms
  bdiiTextField.text = pud.bdii
  proxyURLTextField.text = pud.proxyURL
  proxyCheckBox.selected = pud.proxy
  showProxy(pud.proxy)

  contents += tabbedPane
  
  private def showProxy(b: Boolean) = {
    List(proxyURLLabel, proxyURLLabel, proxyURLTextField).foreach {
      _.visible = b
    }
  }

  override def saveContent(name: String) =
    new GliteEnvironmentDataUI(name,
                               voTextField.text,
                               vomsTextField.text,
                               bdiiTextField.text,
                               proxyCheckBox.selected,
                               proxyURLTextField.text,
                               requirements.architectureCheckBox.selected,
                               requirements.runtimeMemoryTextField.text,
                               requirements.workerNodeMemoryTextField.text,
                               requirements.maxCPUTimeTextField.text,
                               requirements.otherRequirementTextField.text)
}
