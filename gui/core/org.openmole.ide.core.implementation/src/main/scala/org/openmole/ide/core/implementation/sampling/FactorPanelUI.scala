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

package org.openmole.ide.core.implementation.sampling

import scala.swing._
import event.{ FocusGained, SelectionChanged }
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.misc.widget._
import multirow.ComponentFocusedEvent
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.implementation.sampling.DefaultFactor._
import org.openmole.ide.core.implementation.panel.FactorPanel

class FactorPanelUI(factorWidget: IFactorWidget,
                    factorPanel: FactorPanel) extends PluginPanel("") with IPanelUI {

  val domains = KeyRegistry.domains.values

  val domainComboBox = new MyComboBox(domainContent(factorWidget.dataUI.prototype))
  domainComboBox.selection.item = factorWidget.dataUI.domain

  val protoComboBox = new MyComboBox(Proxys.prototypes.toList)
  protoComboBox.selection.item = factorWidget.dataUI.prototype

  var dPanel = factorWidget.dataUI.domain.buildPanelUI(protoComboBox.selection.item)

  val protoDomainPanel = new PluginPanel("wrap") {
    contents += new PluginPanel("wrap 3") {
      contents += protoComboBox
      contents += new Label(" defined on ")
      contents += domainComboBox
    }
    contents += dPanel.peer
  }

  contents += protoDomainPanel

  domainComboBox.selection.reactions += {
    case SelectionChanged(`domainComboBox`) ⇒
      if (protoDomainPanel.contents.size == 2) protoDomainPanel.contents.remove(1)
      dPanel = domainComboBox.selection.item.buildPanelUI(protoComboBox.selection.item)
      listenToDomain
      protoDomainPanel.contents += dPanel.peer
      repaint
  }

  protoComboBox.selection.reactions += {
    case SelectionChanged(`protoComboBox`) ⇒
      if (protoDomainPanel.contents.size == 2) protoDomainPanel.contents.remove(1)
      val dContent = domainContent(protoComboBox.selection.item)
      domainComboBox.peer.setModel(MyComboBox.newConstantModel(dContent))
      displayDomainPanel(dContent)
  }

  def listenToDomain = {
    listenTo(dPanel.help.components.toSeq: _*)
    reactions += {
      case FocusGained(source: Component, _, _) ⇒ dPanel.help.switchTo(source)
      case ComponentFocusedEvent(source: Component) ⇒ dPanel.help.switchTo(source)
    }
    factorPanel.updateHelp
  }

  def displayDomainPanel(dContent: List[IDomainDataUI[_]]) = dContent.filter {
    it ⇒
      domainComboBox.selection.item.toString == it.toString
  }.headOption match {
    case Some(d: IDomainDataUI[_]) ⇒
      dPanel = d.buildPanelUI(protoComboBox.selection.item)
      protoDomainPanel.contents += dPanel.peer
      listenToDomain
    case _ ⇒
  }

  def saveContent = new FactorDataUI(factorWidget.dataUI.id,
    protoComboBox.selection.item,
    dPanel.saveContent,
    factorWidget.dataUI.previousFactor)

}
