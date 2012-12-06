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

import org.openmole.ide.core.model.sampling.{ IDomainWidget, ISamplingWidget }
import org.openmole.ide.core.implementation.panel.{ SamplingPanel }
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.core.model.panel.IPanelUI
import org.openmole.ide.core.implementation.registry.KeyRegistry
import swing.{ Component, MyComboBox }
import org.openmole.ide.core.model.data.ISamplingDataUI
import swing.event.{ FocusGained, SelectionChanged }
import org.openmole.ide.misc.widget.multirow.ComponentFocusedEvent

class SamplingPanelUI(samplingWidget: ISamplingWidget,
                      samplingPanel: SamplingPanel) extends PluginPanel("") with IPanelUI {

  val incomings = samplingWidget.incomings

  val samplings = KeyRegistry.samplings.values.map {
    _.buildDataUI
  }.toList.sorted.filter {
    s ⇒
      incomings.forall {
        _ match {
          case dw: IDomainWidget ⇒ s.isAcceptable(dw.proxy.dataUI)
          case sw: ISamplingWidget ⇒
            s.isAcceptable(sw.proxy.dataUI) &&
              contraintsGreaterThanOrEqual(s.inputNumberConstrainst, sw.proxy.dataUI.inputNumberConstrainst)
          case _ ⇒ false
        }
      }
  }

  val samplingComboBox = new MyComboBox(samplings)
  samplings.filter {
    _.toString == samplingWidget.proxy.dataUI.toString
  }.headOption match {
    case Some(d: ISamplingDataUI) ⇒
      samplingComboBox.selection.item = d
    case _ ⇒
  }

  var sPanel = samplingWidget.proxy.dataUI.buildPanelUI

  val mainSamplingPanel = new PluginPanel("wrap") {
    contents += samplingComboBox
    contents += sPanel.peer
  }

  contents += mainSamplingPanel
  samplingComboBox.selection.reactions += {
    case SelectionChanged(`samplingComboBox`) ⇒
      if (mainSamplingPanel.contents.size == 2) mainSamplingPanel.contents.remove(1)
      sPanel = samplingComboBox.selection.item.buildPanelUI
      listenToSampling
      mainSamplingPanel.contents += sPanel.peer
      repaint
  }

  def listenToSampling = {
    listenTo(sPanel.help.components.toSeq: _*)
    reactions += {
      case FocusGained(source: Component, _, _) ⇒ sPanel.help.switchTo(source)
      case ComponentFocusedEvent(source: Component) ⇒ sPanel.help.switchTo(source)
    }
    samplingPanel.updateHelp
  }

  def saveContent = sPanel.saveContent

  def contraintsGreaterThanOrEqual(c1: Option[Int], c2: Option[Int]) =
    c1 match {
      case Some(i: Int) ⇒ c2 match {
        case Some(j: Int) ⇒ i >= j
        case _ ⇒ false
      }
      case _ ⇒ true
    }
}