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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.model.workflow.{ ICapsuleUI, IMoleScene }
import org.openmole.ide.core.model.panel.PanelMode._
import java.awt.BorderLayout
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.Label

class CapsulePanel(scene: IMoleScene, capsule: ICapsuleUI, index: Int = 0) extends BasePanel(None, scene, EDIT) {

  val panelUI = capsule.dataUI.buildPanelUI(index)

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += new Label { text = "<html><b><font \"size=\"4\" >Capsule settings</font></b></html>" }
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  def create {}

  def delete = true

  def save = {
    capsule.dataUI = panelUI.save
    capsule.update
  }

}