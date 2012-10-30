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
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IConnectorUI
import org.openmole.ide.core.model.workflow.IConnectorUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openmole.ide.misc.widget.multirow.MultiCombo._
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import scala.swing.ScrollPane

object ConnectorPrototypeFilterDialog extends PrototypeDialog {
  def display(connectorUI: IConnectorUI) = {
    openable(connectorUI.source.dataUI) match {
      case true ⇒
        val prototypePanel = new FilteredPrototypePanel(connectorUI)
        if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(prototypePanel) {
          verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
        }.peer,
          "Add prototype filters")).equals(NotifyDescriptor.OK_OPTION)) {
          connectorUI.filteredPrototypes = prototypePanel.multiPrototypeCombo.content.map { _.comboValue.get }
          CheckData.checkMole(connectorUI.source.scene)
        }
      case false ⇒ StatusBar.warn("No Prototype is defined !")
    }
  }

  class FilteredPrototypePanel(connector: IConnectorUI) extends PluginPanel("") {
    preferredSize = new Dimension(250, 300)
    val multiPrototypeCombo = new MultiCombo("Filtered Prototypes",
      connector.availablePrototypes,
      connector.filteredPrototypes.map { fp ⇒ new ComboPanel(connector.availablePrototypes, new ComboData(Some(fp))) },
      CLOSE_IF_EMPTY,
      ADD)
    // if (connector.filteredPrototypes.isEmpty) multiPrototypeCombo.removeAllRows
    contents += multiPrototypeCombo.panel
  }
}