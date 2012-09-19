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

package org.openmole.ide.plugin.domain.range

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.TextField
import scala.swing.Label

class RangeDomainPanelUI(pud: RangeDomainDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val minField = new TextField(6) { text = pud.min }
  val maxField = new TextField(6) { text = pud.max }
  val stepField = new TextField(6) { text = pud.step }

  contents += (new Label("Min"), "gap para")
  contents += minField
  contents += (new Label("Max"), "gap para")
  contents += maxField
  contents += (new Label("Step"), "gap para")
  contents += (stepField, "wrap")

  def saveContent = new RangeDomainDataUI(minField.text,
    maxField.text,
    stepField.text)

  //  override val help = new Helper {
  //    add(minField, new Help(i18n.getString("min"), i18n.getString("minEX")))
  //    add(maxField, new Help(i18n.getString("max"), i18n.getString("maxEX")))
  //    add(stepField, new Help(i18n.getString("step"), i18n.getString("stepEX")))
  //  }
}
