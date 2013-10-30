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

package org.openmole.ide.core.implementation.dialog

import java.awt.Dimension
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openmole.ide.core.implementation.data.{ DomainDataUI, CheckData }
import org.openmole.ide.misc.widget.{ ContentComboBox, PluginPanel }
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import swing.{ Label, MyComboBox, ScrollPane, TextField }
import org.openmole.ide.core.implementation.sampling.{ SamplingOrDomainProxyUI, IFactorProxyUI, SamplingConnectorWidget }
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.tools.util.Types
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.ide.core.implementation.workflow.{ ConnectorUI, CapsuleUI }

object ConnectorPrototypeFilterDialog extends PrototypeDialog {
  def display(connectorUI: ConnectorUI) = {
    if (connectorUI.source.outputs.isEmpty)
      StatusBar().warn("No Prototype is defined !")
    else {
      val prototypePanel = new FilteredPrototypePanel(connectorUI)
      if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(prototypePanel) {
        verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
      }.peer,
        "Add prototypeMap filters")).equals(NotifyDescriptor.OK_OPTION)) {
        connectorUI.filteredPrototypes = prototypePanel.multiPrototypeCombo.content.map {
          _.comboValue.get
        }
        CheckData.checkMole(connectorUI.source.scene)
      }
    }
  }

  class OrderingDialog(compositionProxy: SamplingOrDomainProxyUI,
                       connectorWidget: SamplingConnectorWidget) extends PluginPanel("wrap", "[grow,fill]", "") {
    val orderTextField = new TextField(compositionProxy.ordering.toString, 5)
    contents += orderTextField

    def display: Unit = {
      StatusBar().clear
      if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(this) {
        verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
      }.peer,
        "Order")).equals(NotifyDescriptor.OK_OPTION)) {
        compositionProxy.ordering = orderTextField.text.toInt
        connectorWidget.update
      }
    }
  }

  class FactorPrototypeDialog(connectorWidget: SamplingConnectorWidget) extends PluginPanel("wrap") {
    preferredSize = new Dimension(150, 100)

    def sel = connectorWidget.factorProxyUI match {
      case Some(f: IFactorProxyUI) ⇒ f.dataUI.prototype
      case _                       ⇒ None
    }

    val protoCombo = ContentComboBox(availablePrototypes, sel)
    contents += new Label("Prototype to be applied on the domain")
    contents += protoCombo.widget

    def availablePrototypes =
      connectorWidget.factorProxyUI match {
        case Some(f: IFactorProxyUI) ⇒ Proxies.instance.prototypes.filter {
          p ⇒ assignable(f.dataUI.domain.dataUI.domainType.runtimeClass, p.dataUI.`type`.runtimeClass)
        }.toList
        case _ ⇒ List()
      }

    def display: Unit = {
      StatusBar().clear
      protoCombo.setModel(availablePrototypes, sel)
      if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(this) {
        verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
      }.peer,
        "Prototype")).equals(NotifyDescriptor.OK_OPTION)) {
        connectorWidget.factorProxyUI match {
          case Some(f: IFactorProxyUI) ⇒ f.dataUI.prototype = protoCombo.widget.selection.item.content
          case _                       ⇒
        }
        connectorWidget.update
      }
    }
  }

  class FilteredPrototypePanel(connector: ConnectorUI) extends PluginPanel("") {
    preferredSize = new Dimension(250, 300)
    val multiPrototypeCombo = new MultiCombo("Filtered Prototypes",
      connector.source.outputs,
      connector.filteredPrototypes.map {
        fp ⇒ new ComboPanel(connector.source.outputs, new ComboData(Some(fp)))
      },
      CLOSE_IF_EMPTY,
      ADD)
    contents += multiPrototypeCombo.panel
  }

}
