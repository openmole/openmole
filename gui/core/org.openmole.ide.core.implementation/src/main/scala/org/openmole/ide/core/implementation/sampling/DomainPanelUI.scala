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
import org.openmole.ide.core.model.sampling.{ IModifier, IDomainWidget }
import org.openmole.ide.misc.widget._
import multirow.ComponentFocusedEvent
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.implementation.panel.DomainPanel

class DomainPanelUI(domainWidget: IDomainWidget,
                    domainPanel: DomainPanel) extends PluginPanel("") with IPanelUI {

  val domains = KeyRegistry.domains.values.map {
    _.buildDataUI
  }.toList.sorted.filter {
    d ⇒
      if (domainWidget.incomings.isEmpty) true
      else {
        d match {
          case dm: IModifier ⇒ true
          case _ ⇒ false
        }
      }
  }
  val previous: List[IDomainWidget] = domainWidget.incomings

  val domainComboBox = new MyComboBox(domains)
  domainComboBox.selection.item = domains.filter {
    _.toString == domainWidget.proxy.dataUI.toString
  }.head

  var dPanel = domainWidget.proxy.dataUI.buildPanelUI

  val protoDomainPanel = new PluginPanel("wrap") {
    contents += domainComboBox
    contents += dPanel.peer
  }

  contents += protoDomainPanel

  domainComboBox.selection.reactions += {
    case SelectionChanged(`domainComboBox`) ⇒
      if (protoDomainPanel.contents.size == 2) protoDomainPanel.contents.remove(1)
      dPanel = domainComboBox.selection.item.buildPanelUI
      listenToDomain
      protoDomainPanel.contents += dPanel.peer
      repaint
  }

  def listenToDomain = {
    listenTo(dPanel.help.components.toSeq: _*)
    reactions += {
      case FocusGained(source: Component, _, _) ⇒ dPanel.help.switchTo(source)
      case ComponentFocusedEvent(source: Component) ⇒ dPanel.help.switchTo(source)
    }
    domainPanel.updateHelp
  }

  def displayDomainPanel(dContent: List[IDomainDataUI]) = dContent.filter {
    it ⇒
      domainComboBox.selection.item.toString == it.toString
  }.headOption match {
    case Some(d: IDomainDataUI) ⇒
      dPanel = d.buildPanelUI
      protoDomainPanel.contents += dPanel.peer
      listenToDomain
    case _ ⇒
  }

  def saveContent = dPanel.saveContent match {
    case m: IModifier ⇒ m.clone(previousDomain = previous.map { _.proxy.dataUI })
    case x: Any ⇒ x
  }

}
