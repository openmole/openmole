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

import swing.{ MyComboBox, Label, TextField }
import org.openmole.ide.misc.widget.{ URL, Help, Helper }
import org.openmole.ide.misc.tools.util.Types._

class LogarithmicRangePanelUI(pud: LogarthmicRangeDataUI) extends GenericRangeDomainPanelUI {

  val stepField = new TextField(6) { text = pud.step.getOrElse(1.0).toString }
  minField.text = pud.min
  maxField.text = pud.max

  contents += (new Label("Step"), "gap para")
  contents += stepField

  typeCombo.peer.setModel(MyComboBox.newConstantModel(List(DOUBLE, BIG_DECIMAL)))

  def saveContent = GenericRangeDomainDataUI(minField.text, maxField.text, Some(stepField.text), true, typeCombo.selection.item)

}