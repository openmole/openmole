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
import swing.Swing._
import javax.swing.JScrollPane
import java.awt.BorderLayout
import scala.swing.event.SelectionChanged
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.core.model.sampling.ISamplingWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.tools.image.Images.CLOSE
import org.openmole.ide.core.model.panel._
import scala.swing.BorderPanel.Position._
import scala.collection.JavaConversions._

class SamplingPanel(samplingWidget: ISamplingWidget,
                    scene: IMoleScene,
                    mode: PanelMode.Value) extends BasePanel(None,
  scene,
  mode) {
  val panelUI = samplingWidget.dataUI.buildPanelUI

  peer.add(mainPanel.peer, BorderLayout.NORTH)
  peer.add(new PluginPanel("wrap") {
    contents += panelUI.tabbedPane
    contents += panelUI.help
  }.peer, BorderLayout.CENTER)

  def create = {}

  def delete = true

  def save = {
    samplingWidget.dataUI = panelUI.saveContent
    samplingWidget.update
  }
}