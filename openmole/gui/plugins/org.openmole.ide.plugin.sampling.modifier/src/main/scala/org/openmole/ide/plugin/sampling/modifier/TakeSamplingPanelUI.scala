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
package org.openmole.ide.plugin.sampling.modifier

import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import java.util.{ Locale, ResourceBundle }
import swing.{ Label, TextField }
import org.openmole.ide.core.implementation.panelsettings.ISamplingPanelUI

class TakeSamplingPanelUI(cud: TakeSamplingDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends ISamplingPanelUI {

  val sizeTextField = new TextField(cud.size, 8)

  val components = List(("", new PluginPanel("wrap 2", "", "") {
    contents += new Label("Size")
    contents += sizeTextField
  }))

  override def saveContent = new TakeSamplingDataUI010(sizeTextField.text)

  override lazy val help = new Helper(List(new URL(i18n.getString("takePermalinkText"), i18n.getString("takePermalink"))))
  add(sizeTextField, new Help(i18n.getString("takeSize"), i18n.getString("takeSizeEx")))

}