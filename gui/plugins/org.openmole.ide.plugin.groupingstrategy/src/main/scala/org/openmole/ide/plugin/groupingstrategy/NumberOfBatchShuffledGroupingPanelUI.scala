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

package org.openmole.ide.plugin.groupingstrategy

import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.panel.IGroupingPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.plugin.grouping.batch.BatchShuffledGrouping
import scala.swing.Label
import scala.swing.TextField

class NumberOfBatchShuffledGroupingPanelUI
    extends PluginPanel("wrap")
    with IGroupingPanelUI {

  val numberTextField = new TextField("", 5)
  contents += new Label("Number of jobs / group : ")
  contents += numberTextField

  def coreObject = new BatchShuffledGrouping(numberTextField.text.toInt)
}
