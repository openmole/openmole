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

package org.openmole.ide.plugin.sampling.tools

import scala.swing._
import swing.Swing._
import scala.swing.event.SelectionChanged
import swing.ListView._
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.model.data.IBoundedDomainDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
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
import scala.collection.JavaConversions._

object MultiGenericBoundedSamplingPanel {

  class GenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI],
                                    boundedDomainContent: List[IBoundedDomainDataProxyUI],
                                    data: GenericBoundedSamplingData) extends PluginPanel("wrap")
      with IPanel[GenericBoundedSamplingData] {

    val protoComboBox = new MyComboBox(protoContent)
    val boundedDomainComboBox = new MyComboBox(boundedDomainContent.map { _.toString })
    var dPanel = data.boundedDomainDataUI.getOrElse { boundedDomainContent(0).dataUI }.buildPanelUI

    data.prototypeProxy match {
      case Some(x: IPrototypeDataProxyUI) ⇒ protoComboBox.selection.item = x
      case _ ⇒
    }

    data.boundedDomainProxy match {
      case Some(x: String) ⇒
        boundedDomainComboBox.selection.item = x
      case _ ⇒
    }

    contents += new PluginPanel("wrap 3") {
      contents += protoComboBox
      contents += new Label("defined on ")
      contents += boundedDomainComboBox
    }
    contents += dPanel.peer

    boundedDomainComboBox.selection.reactions += {
      case SelectionChanged(`boundedDomainComboBox`) ⇒
        if (contents.size == 2) contents.remove(1)
        dPanel = boundedDomainContent.filter { it ⇒
          boundedDomainComboBox.selection.item == it.toString
        }.head.dataUI.buildPanelUI
        contents += dPanel.peer
    }

    def content = {
      new GenericBoundedSamplingData(Some(protoComboBox.selection.item),
        Some(boundedDomainComboBox.selection.item),
        Some(dPanel.saveContent("")))
    }
  }

  class GenericBoundedSamplingData(val prototypeProxy: Option[IPrototypeDataProxyUI] = None,
                                   val boundedDomainProxy: Option[String] = None,
                                   val boundedDomainDataUI: Option[IBoundedDomainDataUI] = None) extends IData

  class GenericBoundedSamplingFactory(protoContent: List[IPrototypeDataProxyUI],
                                      boundedDomainContent: List[IBoundedDomainDataProxyUI]) extends IFactory[GenericBoundedSamplingData] {
    def apply = new GenericBoundedSamplingPanel(protoContent,
      boundedDomainContent,
      new GenericBoundedSamplingData)
  }

}
import MultiGenericBoundedSamplingPanel._
class MultiGenericBoundedSamplingPanel(protoContent: List[IPrototypeDataProxyUI] = List.empty,
                                       boundedDomainContent: List[IBoundedDomainDataProxyUI] = List.empty,
                                       initPanels: List[GenericBoundedSamplingPanel]) extends MultiPanel("Factors",
  new GenericBoundedSamplingFactory(protoContent, boundedDomainContent),
  initPanels) {
}