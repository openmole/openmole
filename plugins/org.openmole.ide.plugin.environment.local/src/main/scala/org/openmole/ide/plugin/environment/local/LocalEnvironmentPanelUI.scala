/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.environment.local

import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class LocalEnvironmentPanelUI(pud: LocalEnvironmentDataUI) extends PluginPanel("wrap 2") with IEnvironmentPanelUI {
  val nbThreadTextField = new TextField(6)
  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += (new Label("Number of threads"), "gap para")
    contents += nbThreadTextField
  })

  nbThreadTextField.text = pud.nbThread.toString

  override def saveContent(name: String) = new LocalEnvironmentDataUI(name,
    nbThreadTextField.text.toInt)
}
