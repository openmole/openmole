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

package org.openmole.ide.core.implementation.panelsettings

import java.awt.Dimension
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.RadioButton
import org.openmole.misc.workspace.Workspace
import org.openmole.ide.core.implementation.workflow.IMoleUI
import org.openmole.ide.core.implementation.panel.{ AnonSaveSettings, Settings }

trait MolePanelUI extends Settings with AnonSaveSettings {

  def dataUI: IMoleUI

  type DATAUI = IMoleUI

  override val panel = new PluginPanel("wrap") {
    minimumSize = new Dimension(300, 400)
    preferredSize = new Dimension(300, 400)
    Workspace.pluginDirLocation.list.foreach {
      f ⇒
        contents += new RadioButton(f) {
          selected = dataUI.plugins.toList.contains(f)
        }
    }
  }

  val components = List()

  def saveContent: DATAUI = {
    dataUI.plugins = panel.contents.flatMap {
      c ⇒
        c match {
          case x: RadioButton ⇒ List(x)
          case _              ⇒ Nil
        }
    }.toList.filter {
      _.selected
    }.map {
      _.text
    }
    dataUI
  }
}
