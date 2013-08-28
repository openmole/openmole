/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.domain.file

import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import swing.{ CheckBox, TextField }
import java.util.{ Locale, ResourceBundle }
import org.openmole.ide.core.implementation.panelsettings.IDomainPanelUI

class ListFilesDomainPanelUI(val dataUI: ListFilesDomainDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends IDomainPanelUI with FileDomainPanelUI {

  val dirTField = directoryTextField(dataUI.directoryPath)
  val regexpTextField = new TextField(8) {
    text = dataUI.regexp
  }
  val recursiveCheckBox = new CheckBox("Recursive") {
    selected = dataUI.recursive
  }

  val components = List(("", new PluginPanel("wrap") {
    contents += FileDomainPanelUI.panel(List((dirTField, "Directory"), (regexpTextField, "wrap"), (recursiveCheckBox, "")))
  }))

  override def toString = dataUI.name

  def saveContent = new ListFilesDomainDataUI(dirTField.text, regexpTextField.text, recursiveCheckBox.selected)

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(dirTField, new Help(i18n.getString("dir"), i18n.getString("dirEx")))
  add(regexpTextField, new Help(i18n.getString("regularExp"), i18n.getString("regularExpEx")))
  add(recursiveCheckBox, new Help(i18n.getString("recursive"), i18n.getString("recursiveEx")))

}