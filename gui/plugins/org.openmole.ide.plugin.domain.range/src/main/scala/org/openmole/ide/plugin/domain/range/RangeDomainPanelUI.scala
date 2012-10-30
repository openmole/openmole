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
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.IDomainPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.event._
import scala.swing.TextField
import scala.swing.CheckBox
import scala.swing.Label

class RangeDomainPanelUI(pud: GenericRangeDomainDataUI[_],
                         //  prototype: IPrototypeDataProxyUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IDomainPanelUI {
                         prototype: IPrototypeDataProxyUI) extends PluginPanel("wrap 2") with IDomainPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val minField = new TextField(6) { text = pud.min }
  val maxField = new TextField(6) { text = pud.max }
  val stepCheckBox = new CheckBox("Step")
  val stepField = new TextField(6) { text = pud.stepString }
  val logCheckBox = new CheckBox("Logarithmic")

  stepField.visible = stepContent.isDefined
  stepCheckBox.selected = stepContent.isDefined

  listenTo(`stepCheckBox`)
  reactions += {
    case ButtonClicked(`stepCheckBox`) ⇒ stepField.visible = stepCheckBox.selected
  }

  contents += (new Label("Min"), "gap para")
  contents += minField
  contents += (new Label("Max"), "gap para")
  contents += maxField
  contents += (stepCheckBox, "gap para")
  contents += stepField
  contents += logCheckBox

  logCheckBox.visible = (prototype.dataUI.toString == "BigDecimal" ||
    prototype.dataUI.toString == "Double")

  def stepContent: Option[String] = {
    if (stepCheckBox.selected) {
      if (stepField.text.isEmpty) None
      else Some(stepField.text)
    } else None
  }

  def saveContent = GenericRangeDomainDataUI(minField.text,
    maxField.text,
    stepContent,
    logCheckBox.selected,
    prototype.dataUI.toString)
}
