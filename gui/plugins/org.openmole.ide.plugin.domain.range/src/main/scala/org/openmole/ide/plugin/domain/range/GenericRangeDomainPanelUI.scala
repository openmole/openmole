/*
 * Copyright (C) 2012 Mathieu Leclaire 
 * < mathieu.leclaire at openmole.org >
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
package org.openmole.ide.plugin.domain.range

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import swing.{ MyComboBox, TextField, Label }
import org.openmole.ide.misc.tools.util.Types._

abstract class GenericRangeDomainPanelUI extends PluginPanel("wrap 2") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val typeCombo = new MyComboBox(List(DOUBLE))
  val minField = new TextField(6)
  val maxField = new TextField(6)

  contents += (new Label("Type"), "gap para")
  contents += (typeCombo, "span 2")
  contents += (new Label("Min"), "gap para")
  contents += minField
  contents += (new Label("Max"), "gap para")
  contents += maxField

  minField.peer
  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(minField, new Help(i18n.getString("min"), i18n.getString("minEx")))
    add(maxField, new Help(i18n.getString("max"), i18n.getString("maxEx")))
  }
}
