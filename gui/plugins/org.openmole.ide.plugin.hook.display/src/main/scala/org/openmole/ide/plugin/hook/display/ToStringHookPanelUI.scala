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

package org.openmole.ide.plugin.hook.display

import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.plugin.misc.tools.MultiPrototypePanel
import org.openmole.ide.core.implementation.dataproxy.Proxys
import swing.TabbedPane
import org.openmole.ide.misc.widget.PluginPanel

class ToStringHookPanelUI(dataUI: ToStringHookDataUI) extends PluginPanel("") with IHookPanelUI {

  println("Protot list : " + Proxys.prototypes.toList)
  val combo = new MultiPrototypePanel("Display prototypes",
    dataUI.toBeHooked,
    Proxys.prototypes.toList)

  contents += combo

  tabbedPane.pages.insert(0, new TabbedPane.Page("Prototypes", this))

  def saveContent(name: String) = new ToStringHookDataUI(name,
    combo.multiPrototypeCombo.content.map { _.comboValue.get }.filter { _ != null })
}