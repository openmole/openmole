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
package org.openmole.ide.core.implementation.sampling

import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.implementation.registry.KeyRegistry
import scala.swing.{ Publisher, Label, Component, MyComboBox }
import swing.event.{ FocusGained, SelectionChanged }
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent
import javax.swing.ImageIcon
import javax.imageio.ImageIO
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.data.SamplingDataUI
import org.openmole.ide.core.implementation.panel.{ AnonSaveSettings, Settings }

class SamplingPanelUI(samplingWidget: ISamplingWidget) extends Settings with AnonSaveSettings {

  type DATAUI = SamplingDataUI
  val incomings = samplingWidget.incomings

  val samplings =
    KeyRegistry.samplings.values.map {
      _.buildDataUI
    }.toList.sorted.filter {
      s ⇒
        incomings.forall {
          _ match {
            case dw: IDomainWidget   ⇒ s.isAcceptable(dw.proxy.dataUI)
            case sw: ISamplingWidget ⇒ s.isAcceptable(sw.proxy.dataUI)
            case _                   ⇒ false
          }
        }
    }.filter {
      s ⇒ testConstraints(s.inputNumberConstrainst)
    }

  StatusBar().clear

  val samplingComboBox = new MyComboBox(samplings) {
    peer.setMaximumRowCount(15)
  }
  samplings.filter {
    _.toString == samplingWidget.proxy.dataUI.toString
  }.headOption match {
    case Some(d: SamplingDataUI) ⇒
      samplingComboBox.selection.item = d
    case _ ⇒
  }

  var sPanel = samplingWidget.proxy.dataUI.buildPanelUI

  val mainSamplingPanel = new PluginPanel("wrap") {
    contents += samplingComboBox
    contents += buildPanel(samplingWidget.proxy.dataUI)
  }

  val components = List(("Settings", mainSamplingPanel))

  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`) ⇒
      if (mainSamplingPanel.contents.size == 2) mainSamplingPanel.contents.remove(1)
      mainSamplingPanel.contents += buildPanel(samplingComboBox.selection.item)
      listenToSampling
    // repaint
  }

  def buildPanel(s: SamplingDataUI) = new PluginPanel("wrap 2") {
    contents += new Label {
      icon = new ImageIcon(ImageIO.read(s.getClass.getClassLoader.getResource(s.fatImagePath)))
    }
    sPanel = s.buildPanelUI
    contents += sPanel.panel.peer
  }

  def listenToSampling = {
    listenTo(sPanel.help.components.toSeq: _*)
    reactions += {
      case FocusGained(source: Component, _, _)     ⇒ sPanel.help.switchTo(source)
      case ComponentFocusedEvent(source: Component) ⇒ sPanel.help.switchTo(source)
    }
    // samplingPanel.updateHelp
  }

  def saveContent = sPanel.saveContent

  def testConstraints(c: Option[Int]) = {
    c match {
      case Some(i: Int) ⇒ i >= incomings.size
      case _            ⇒ true
    }
  }
}