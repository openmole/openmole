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

package org.openmole.ide.core.implementation.workflow.sampling

import scala.swing.Panel
import java.awt.Color
import java.awt.Dimension
import java.awt.BorderLayout
import scala.swing.MyComboBox
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.data.FactorDataUI
import scala.swing.event.SelectionChanged

class FactorWidget(factor: FactorDataUI) extends Panel {
  background = Color.BLUE
  preferredSize = new Dimension(200, 50)

  peer.setLayout(new BorderLayout)

  val prototypeComboBox = new MyComboBox(prototypeContent(factor.domain))
  val domainComboBox = new MyComboBox(domainContent(factor.prototype))

  listenTo(domainComboBox.selection)
  reactions += {
    case SelectionChanged(`domainComboBox`) ⇒
      prototypeComboBox.peer.setModel(MyComboBox.newConstantModel(prototypeContent(Some(domainComboBox.selection.item))))
  }

  peer.add(new MigPanel("") {
    contents += prototypeComboBox
    contents += domainComboBox
  }.peer, BorderLayout.NORTH)

  def prototypeContent(domain: Option[IDomainDataUI]) = Proxys.prototypes.filter { p ⇒
    domain match {
      case Some(d: IDomainDataUI) ⇒ d.isAcceptable(p)
      case _ ⇒ false
    }
  }.toList

  def domainContent(prototype: Option[IPrototypeDataProxyUI]) =
    prototype match {
      case Some(p: IPrototypeDataProxyUI) ⇒ KeyRegistry.domains.values.map { _.buildDataUI }.filter { _.isAcceptable(p) }.toList
      case _ ⇒ List.empty
    }
}