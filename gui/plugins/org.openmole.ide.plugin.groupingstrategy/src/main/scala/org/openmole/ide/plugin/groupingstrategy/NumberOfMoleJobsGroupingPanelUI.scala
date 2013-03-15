/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.groupingstrategy

import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.core.model.panel.IGroupingPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.plugin.grouping.batch.MoleJobsGrouping
import scala.swing.Label
import scala.swing.TextField

class NumberOfMoleJobsGroupingPanelUI(dataUI: NumberOfMoleJobsGroupingDataUI) extends PluginPanel("wrap 2") with IGroupingPanelUI {

  val numberTextField = new TextField(dataUI.number.toString, 5)
  contents += new Label("jobs / group : ")
  contents += numberTextField

  def saveContent = new NumberOfMoleJobsGroupingDataUI(numberTextField.text.toInt)
}
