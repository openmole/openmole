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

package org.openmole.ide.core.implementation.panel

import java.awt.Dimension
import org.openmole.ide.core.implementation.data.MoleDataUI
import org.openmole.ide.core.model.data.IMoleDataUI
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import scala.swing.FileChooser.SelectionMode._

class MolePanelUI(mdu: IMoleDataUI) extends PluginPanel("") with IPanelUI {
  minimumSize = new Dimension(300, 400)
  preferredSize = new Dimension(300, 400)
  val pluginMultiTextField = new MultiChooseFileTextField("Plugin",
    mdu.plugins.toList,
    "Select a file", Some("Plugin files"), FilesOnly, Some("jar")) {
    tooltip = Help.tooltip("Plugin path. Can be used to link a jar file for instance", "/home/path/to/myjar.jar")
  }
  contents += pluginMultiTextField.panel

  def saveContent(name: String) = new MoleDataUI(pluginMultiTextField.content)
}
