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

import javax.swing._
import javax.swing.JOptionPane._
import scala.swing.ScrollPane
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.workflow.DataChannelConnectionWidget
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.multirow.MultiCombo
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.awt.StatusDisplayer

object DataChannelDialog {
  def display(dcWidget : DataChannelConnectionWidget) = {
    Proxys.prototypes.isEmpty match{
      case false=>
        val prototypePanel = new PrototypePanel(dcWidget.dataChannelUI.prototypes)
        if (DialogDisplayer.getDefault.notify(new DialogDescriptor(new ScrollPane(prototypePanel){size.height = 200}.peer, "Set the Data Channel")).equals(NotifyDescriptor.OK_OPTION)) 
          dcWidget.dataChannelUI.prototypes = prototypePanel.multiPrototypeCombo.content
      case true=> StatusDisplayer.getDefault.setStatusText("No Prototype is defined !")
    }
  }
  
  class PrototypePanel(protoProxys: List[IPrototypeDataProxyUI]) extends MigPanel("") {
    val multiPrototypeCombo = new MultiCombo("Prototypes",Proxys.prototypes.toList,protoProxys)
    contents+= multiPrototypeCombo.panel
  }
}
