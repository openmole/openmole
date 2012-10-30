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
import swing.Swing._
import java.awt.BorderLayout
import scala.swing.event.SelectionChanged
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.core.model.sampling.IFactorWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget._
import org.openmole.ide.misc.tools.image.Images.CLOSE
import org.openmole.ide.core.model.panel._
import scala.swing.BorderPanel.Position._
import scala.collection.JavaConversions._

class FactorPanelUI(factorWidget: IFactorWidget) extends PluginPanel("") with IPanelUI {

  val domains = KeyRegistry.domains.values.map { _.buildDataUI }.toList
  val protoComboBox = new MyComboBox(prototypeContent(factorWidget.dataUI.domain))
  val domainComboBox = new MyComboBox(domainContent(factorWidget.dataUI.prototype))
  var dPanel = factorWidget.dataUI.domain.getOrElse { domainContent(factorWidget.dataUI.prototype)(0) }.buildPanelUI(protoComboBox.selection.item)
  protoComboBox.peer.setModel(MyComboBox.newConstantModel(prototypeContent(Some(domainComboBox.selection.item))))

  factorWidget.dataUI.prototype match {
    case Some(x: IPrototypeDataProxyUI) ⇒ protoComboBox.selection.item = x
    case _ ⇒
  }

  factorWidget.dataUI.domain match {
    case Some(x: IDomainDataUI[_]) ⇒
      domainComboBox.selection.item = x
    case _ ⇒
  }

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
      protoComboBox.peer.setModel(MyComboBox.newConstantModel(prototypeContent(Some(domainComboBox.selection.item))))
      dPanel = domainComboBox.selection.item.buildPanelUI(protoComboBox.selection.item)
      protoDomainPanel.contents += dPanel.peer
  }

  protoComboBox.selection.reactions += {
    case SelectionChanged(`protoComboBox`) ⇒
      if (protoDomainPanel.contents.size == 2) protoDomainPanel.contents.remove(1)
      val dContent = domainContent(Some(protoComboBox.selection.item))
      // domainComboBox.peer.setModel(MyComboBox.newConstantModel(dContent))
      displayDomainPanel(dContent)
  }

  def displayDomainPanel(dContent: List[IDomainDataUI[_]]) = dContent.filter { it ⇒
    domainComboBox.selection.item.toString == it.toString
  }.headOption match {
    case Some(d: IDomainDataUI[_]) ⇒
      dPanel = d.buildPanelUI(protoComboBox.selection.item)
      protoDomainPanel.contents += dPanel.peer
    case _ ⇒
  }

  def prototypeContent(domain: Option[IDomainDataUI[_]]) = Proxys.prototypes.filter { p ⇒
    domain match {
      case Some(d: IDomainDataUI[_]) ⇒ d.isAcceptable(p)
      case _ ⇒ true
    }
  }.toList

  def domainContent(proto: Option[IPrototypeDataProxyUI]) =
    proto match {
      case Some(p: IPrototypeDataProxyUI) ⇒ domains.filter { _.isAcceptable(p) }
      case _ ⇒ domains
    }

  def saveContent = new FactorDataUI(factorWidget.dataUI.id,
    Some(protoComboBox.selection.item),
    Some(dPanel.saveContent))

}
