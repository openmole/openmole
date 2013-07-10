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
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.{ Label, TextField }

class DiracEnvironmentPanelUI(pud: DiracEnvironmentDataUI) extends PluginPanel("wrap 2") with IEnvironmentPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _             ⇒ ""
  }

  implicit def stringToStringOpt(s: String) = s.isEmpty match {
    case true  ⇒ None
    case false ⇒ Some(s)
  }

  val vo = new VO(pud.voName, pud.vomsURL, pud.bdii)
  val serviceTextField = new TextField(pud.service, 20)
  val groupTextField = new TextField(pud.group, 20)
  val setupTextField = new TextField(pud.setup, 20)
  val fqanTextField = new TextField(pud.fqan.getOrElse(""), 20)
  val cpuTimeTextField = new TextField(pud.cpuTime.getOrElse(""), 20)
  val openMOLEMemoryTextField = new TextField(pud.openMOLEMemory.getOrElse("").toString, 20)

  val components = List(("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("VO"), "gap para")
    contents += vo.voComboBox
    contents += (new Label("Service"), "gap para")
    contents += serviceTextField
    contents += (new Label("Group"), "gap para")
    contents += groupTextField
    contents += (new Label("BDII"), "gap para")
    contents += vo.bdiiTextField
    contents += (new Label("VOMS"), "gap para")
    contents += vo.vomsTextField
    contents += (new Label("Setup"), "gap para")
    contents += setupTextField
    contents += (new Label("Fqan"), "gap para")
    contents += fqanTextField
    contents += (new Label("CPU Time"), "gap para")
    contents += cpuTimeTextField
    contents += (new Label("OpenMOLE memory"), "gap para")
    contents += openMOLEMemoryTextField
  }))

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
  /*{
    add(nbThreadTextField, new Help(i18n.getString("thread"), i18n.getString("threadEx")))
  } */

  override def saveContent(name: String) = new DiracEnvironmentDataUI(name,
    vo.voComboBox.selection.item,
    serviceTextField.text,
    groupTextField.text,
    vo.bdiiTextField.text,
    vo.vomsTextField.text,
    setupTextField.text,
    Some(fqanTextField.text),
    Some(cpuTimeTextField.text),
    Some(openMOLEMemoryTextField.text.toInt))
}