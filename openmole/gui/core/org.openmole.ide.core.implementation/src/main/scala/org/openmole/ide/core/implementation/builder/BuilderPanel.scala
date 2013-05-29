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
package org.openmole.ide.core.implementation.builder

import org.openmole.ide.misc.widget.PluginPanel
import swing.{ Action, Button, TextField, Label }
import org.openmole.ide.core.model.panel.IBuilderPanelUI
import java.awt.{ Dimension, Color }

abstract class BuilderPanel extends PluginPanel("wrap 2 ") with IBuilderPanelUI {
  val nameTextField = new TextField(10)

  contents += new Label("Name")
  contents += nameTextField

  preferredSize = new Dimension(300, 80)

}