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

package org.openmole.ide.core.implementation.dialog

import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.panel.MolePanelUI
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.swing.FileChooser.SelectionMode._
import scala.swing.ScrollPane

object MoleSettingsDialog {
  def display(manager: IMoleSceneManager) = {
    val settingsPanel = new MolePanelUI(manager.dataUI)
    if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(settingsPanel) {
      verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
    }.peer,
      "Mole plugins")).equals(NotifyDescriptor.OK_OPTION)) {
      manager.dataUI = settingsPanel.saveContent("moleDataUI")
    }
  }

}

