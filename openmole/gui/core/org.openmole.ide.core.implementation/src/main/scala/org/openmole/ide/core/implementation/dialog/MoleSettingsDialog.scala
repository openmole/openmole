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
import scala.swing.ScrollPane
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.implementation.panelsettings.MolePanelUI
import org.openmole.ide.core.implementation.data.CheckData

object MoleSettingsDialog {
  def display(scene: MoleScene) = {
    val settingsPanel = new MolePanelUI {
      def dataUI = scene.dataUI
    }

    val dd = new DialogDescriptor(new ScrollPane(settingsPanel.tabbed) {
      verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
    }.peer, "Preferences")
    dd.setOptions(List(NotifyDescriptor.OK_OPTION).toArray)
    val notification = DialogDisplayer.getDefault.notify(dd)
    if (notification == -1 || notification == 0) {
      settingsPanel.saveContent
      CheckData.checkMole(scene)
    }
  }

}

