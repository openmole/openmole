/*
 * Copyright (C) 2013 mathieu
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

package org.openmole.ide.core.implementation.execution

import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.MyComboBox
import scala.swing.event.SelectionChanged
import org.openmole.ide.core.model.data.IGroupingDataUI

class GroupingStrategyPanelUI(dataUI: Option[IGroupingDataUI]) extends PluginPanel("wrap") {

  val dataUIs = KeyRegistry.groupingStrategies.values.toList.map { _.buildDataUI }
  val groupingFactoryComboBox = new MyComboBox(dataUIs)
  var panelUI = dataUI match {
    case Some(gd: IGroupingDataUI) ⇒
      val f = dataUIs.filter { _.name == gd.name }
      if (!f.isEmpty) groupingFactoryComboBox.selection.item = f.head
      gd.buildPanelUI
    case _ ⇒ buildPanelUI
  }

  contents += groupingFactoryComboBox
  contents += panelUI.peer
  def buildPanelUI = groupingFactoryComboBox.selection.item.buildPanelUI

  groupingFactoryComboBox.selection.reactions += {
    case SelectionChanged(`groupingFactoryComboBox`) ⇒
      panelUI = buildPanelUI
      if (contents.size > 1) contents.remove(1)
      contents += panelUI.peer
    case _ ⇒
  }

  def save = Some(panelUI.saveContent)
}
