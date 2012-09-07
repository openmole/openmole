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

import scala.swing._
import swing.Swing._
import scala.swing.event.SelectionChanged
import swing.ListView._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.IData
import org.openmole.ide.misc.widget.multirow.IFactory
import org.openmole.ide.misc.widget.multirow.IPanel
import org.openmole.ide.misc.widget.multirow.MultiPanel
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.swing.BorderPanel.Position._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.collection.JavaConversions._

object MultiGenericSamplingPanel {

  class GenericSamplingPanel(protoContent: List[IPrototypeDataProxyUI],
                             domainContent: List[IDomainDataUI],
                             data: GenericSamplingData) extends PluginPanel("wrap")
      with IPanel[GenericSamplingData] {

    val protoComboBox = new MyComboBox(protoContent)
    val domainComboBox = new MyComboBox(domainContent)
    var dPanel = data.factor.domain.getOrElse { domainContent(0) }.buildPanelUI

    data.factor.prototype match {
      case Some(x: IPrototypeDataProxyUI) ⇒ protoComboBox.selection.item = x
      case _ ⇒
    }

    data.factor.domain match {
      case Some(x: IDomainDataUI) ⇒
        domainComboBox.selection.item = x
      case _ ⇒
    }

    contents += new PluginPanel("wrap 3") {
      contents += protoComboBox
      contents += new Label("defined on ")
      contents += domainComboBox
    }
    contents += dPanel.peer

    domainComboBox.selection.reactions += {
      case SelectionChanged(`domainComboBox`) ⇒
        if (contents.size == 2) contents.remove(1)
        dPanel = domainContent.filter { it ⇒
          domainComboBox.selection.item == it.toString
        }.head.buildPanelUI
        contents += dPanel.peer
    }

    def content = {
      new GenericSamplingData(new FactorDataUI(Some(protoComboBox.selection.item),
        Some(dPanel.saveContent(""))))
    }
  }

  class GenericSamplingData(val factor: IFactorDataUI) extends IData

  class GenericSamplingFactory(protoContent: List[IPrototypeDataProxyUI],
                               domainContent: List[IDomainDataUI]) extends IFactory[GenericSamplingData] {
    def apply = new GenericSamplingPanel(protoContent,
      domainContent,
      new GenericSamplingData(factor = new FactorDataUI))
  }
}

import MultiGenericSamplingPanel._
class MultiGenericSamplingPanel(protoContent: List[IPrototypeDataProxyUI] = List.empty,
                                domainContent: List[IDomainDataUI] = List.empty,
                                initPanels: List[GenericSamplingPanel]) extends MultiPanel("Factors",
  new GenericSamplingFactory(protoContent, domainContent),
  initPanels)