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

import scala.swing._
import event.FocusGained
import swing.Swing._
import javax.swing.{ ImageIcon, JScrollPane }
import java.awt.BorderLayout
import org.openmole.ide.core.model.sampling.ISamplingWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import multirow.ComponentFocusedEvent
import org.openmole.ide.core.model.panel._
import javax.imageio.ImageIO

class SamplingPanel(samplingWidget: ISamplingWidget,
                    scene: IMoleScene,
                    mode: PanelMode.Value) extends BasePanel(None,
  scene,
  mode) {
  val panelUI = samplingWidget.dataUI.buildPanelUI

  listenTo(panelUI.help.components.toSeq: _*)
  reactions += {
    case FocusGained(source: Component, _, _) ⇒ panelUI.help.switchTo(source)
    case ComponentFocusedEvent(source: Component) ⇒ panelUI.help.switchTo(source)
  }

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap 2") {
    contents += new Label {
      icon = new ImageIcon(ImageIO.read(samplingWidget.dataUI.getClass.getClassLoader.getResource(samplingWidget.dataUI.fatImagePath)))
    }
    contents += panelUI.peer
  }.peer, BorderLayout.CENTER)
  peer.add(panelUI.help.peer, BorderLayout.SOUTH)

  def create = {}

  def delete = true

  def save = {
    samplingWidget.dataUI = panelUI.saveContent
    samplingWidget.update
  }
}