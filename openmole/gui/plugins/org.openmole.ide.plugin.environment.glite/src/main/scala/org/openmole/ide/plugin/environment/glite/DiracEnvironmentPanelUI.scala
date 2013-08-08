/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.{ Label, TextField }

import Converters._
import org.openmole.ide.core.implementation.panelsettings.EnvironmentPanelUI

class DiracEnvironmentPanelUI(pud: DiracEnvironmentDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends EnvironmentPanelUI {

  val vo = new VOPanel(pud.voName, pud.vomsURL, pud.bdii)
  val serviceTextField = new TextField(pud.service, 20)
  val groupTextField = new TextField(pud.group, 20)
  val setupTextField = new TextField(pud.setup, 20)
  val fqanTextField = new TextField(pud.fqan.getOrElse(""), 20)
  val cpuTimeTextField = new TextField(pud.cpuTime.getOrElse(""), 20)
  val openMOLEMemoryTextField = new TextField(pud.openMOLEMemory.getOrElse("").toString, 20)

  val components = List(
    ("Settings", new PluginPanel("wrap 2") {
      contents += (new Label("VO"), "gap para")
      contents += vo.voComboBox
      contents += (new Label("Service"), "gap para")
      contents += serviceTextField
      contents += (new Label("BDII"), "gap para")
      contents += vo.bdiiTextField
      contents += (new Label("VOMS"), "gap para")
      contents += vo.vomsTextField
      contents += vo.enrollmentURLLink
      contents += vo.enrollmentURLLabel
    }),
    ("Options", new PluginPanel("wrap 2") {
      contents += (new Label("Group"), "gap para")
      contents += groupTextField
      contents += (new Label("Setup"), "gap para")
      contents += setupTextField
      contents += (new Label("Fqan"), "gap para")
      contents += fqanTextField
      contents += (new Label("CPU Time"), "gap para")
      contents += cpuTimeTextField
      contents += (new Label("OpenMOLE memory"), "gap para")
      contents += openMOLEMemoryTextField
    }))

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(vo.voComboBox, new Help(i18n.getString("vo"), i18n.getString("voEx")))
  add(vo.vomsTextField, new Help(i18n.getString("voms"), i18n.getString("vomsEx")))
  add(vo.bdiiTextField, new Help(i18n.getString("bdii"), i18n.getString("bdiiEx")))

  override def saveContent(name: String) = new DiracEnvironmentDataUI(name,
    vo.voComboBox.selection.item,
    serviceTextField.text,
    groupTextField.text,
    vo.bdiiTextField.text,
    vo.vomsTextField.text,
    setupTextField.text,
    fqanTextField.text,
    cpuTimeTextField.text,
    openMOLEMemoryTextField.text)
}