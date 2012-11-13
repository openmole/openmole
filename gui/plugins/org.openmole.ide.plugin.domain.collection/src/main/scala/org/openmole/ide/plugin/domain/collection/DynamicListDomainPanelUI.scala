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

package org.openmole.ide.plugin.domain.collection

import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import swing.ScrollPane.BarPolicy._
import swing._
import java.awt.Color
import java.util.{ Locale, ResourceBundle }

class DynamicListDomainPanelUI(pud: DynamicListDomainDataUI[_]) extends PluginPanel("") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val typeCombo = new MyComboBox(pud.availableTypes)
  val textArea = new TextArea(pud.values.mkString("\n"), 10, 20) {
    override val foreground = Color.black
  }

  contents += new ScrollPane(textArea) {
    horizontalScrollBarPolicy = Never
    verticalScrollBarPolicy = AsNeeded
  }

  def saveContent = DynamicListDomainDataUI(textArea.text.split('\n').toSet.toList,
    typeCombo.selection.item)

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(textArea, new Help(i18n.getString("valueList"), i18n.getString("valueListEx")))
  }
}
