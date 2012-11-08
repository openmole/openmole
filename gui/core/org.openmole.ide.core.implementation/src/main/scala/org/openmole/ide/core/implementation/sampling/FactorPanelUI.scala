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
import org.openmole.misc.exception.UserBadDataError

class FactorPanelUI(factorWidget: IFactorWidget) extends PluginPanel("") with IPanelUI {

  val domains = KeyRegistry.domains.values

  val domainComboBox = new MyComboBox(domainContent(factorWidget.dataUI.prototype.getOrElse(Proxys.prototypes.head)))
  factorWidget.dataUI.domain match {
    case Some(x: IDomainDataUI[_]) ⇒
      println("and set " + domainComboBox.selection.item)
      domainComboBox.selection.item = x
      println("and after" + domainComboBox.selection.item)
    case _ ⇒
  }

  val protoComboBox = new MyComboBox(Proxys.prototypes.toList)
  factorWidget.dataUI.prototype match {
    case Some(x: IPrototypeDataProxyUI) ⇒ protoComboBox.selection.item = x
    case _ ⇒
  }

  var dPanel = factorWidget.dataUI.domain.getOrElse {
    domainComboBox.selection.item
  }.buildPanelUI(protoComboBox.selection.item)

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

  def displayDomainPanel(dContent: List[IDomainDataUI[_]]) = dContent.filter {
    it ⇒
      domainComboBox.selection.item.toString == it.toString
  }.headOption match {
    case Some(d: IDomainDataUI[_]) ⇒
      dPanel = d.buildPanelUI(protoComboBox.selection.item)
      protoDomainPanel.contents += dPanel.peer
    case _ ⇒
  }

  def domainContent(proto: IPrototypeDataProxyUI) = {
    println("domain Conmetnt :: " + domains.map {
      _.buildDataUI
    }.filter(_.isAcceptable(proto)).toList)
    domains.map {
      _.buildDataUI
    }.filter(_.isAcceptable(proto)).toList
  }

  def saveContent = new FactorDataUI(factorWidget.dataUI.id,
    Some(protoComboBox.selection.item),
    Some(dPanel.saveContent),
    factorWidget.dataUI.previousFactor)

}
