/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.implementation.PrototypeUI

class DnDAddPrototypeProvider(molescene: MoleScene, view: CapsuleViewUI) extends DnDProvider(molescene){

  var encapsulated= false
  
  override def isAcceptable(widget: Widget,point: Point,transferable: Transferable): ConnectorState=  {
    var state= ConnectorState.REJECT
    if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_DATA_FLAVOR) && encapsulated == true) {
      state = ConnectorState.ACCEPT
    } else if (transferable.isDataFlavorSupported(Constants.SAMPLING_DATA_FLAVOR)){
      println("sampling")
    }
    state
  }
  
  override def accept(widget: Widget, point: Point,t: Transferable)= {
    val proto = t.getTransferData(Constants.PROTOTYPE_DATA_FLAVOR).asInstanceOf[PrototypeUI]
    if (point.x < view.connectableWidget.widgetWidth / 2) {
      view.capsuleModel.taskUI.get.addPrototype(proto, IOType.INPUT)
    } else {
      view.capsuleModel.taskUI.get.addPrototype(proto, IOType.OUTPUT)
    }
  }
}
  
//public class DnDAddPrototypeProvider extends DnDProvider {
//
//    protected boolean encapsulated = false;
//    protected CapsuleViewUI view;
//
//    public DnDAddPrototypeProvider(MoleScene molescene,
//            CapsuleViewUI view) {
//        super(molescene);
//        this.view = view;
//    }
//
//    public void setEncapsulated(boolean encapsulated) {
//        this.encapsulated = encapsulated;
//    }
//
//    @Override
//    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
//        ConnectorState state = ConnectorState.REJECT;
//        if (transferable.isDataFlavorSupported(Constants.PROTOTYPE_DATA_FLAVOR)
//                && encapsulated == true) {
//            state = ConnectorState.ACCEPT;
//        } else if (transferable.isDataFlavorSupported(Constants.SAMPLING_DATA_FLAVOR)) {
//            System.out.println("sampling ");
//        }
//        return state;
//    }
//
//    @Override
//    public void accept(Widget widget, Point point, Transferable t) {
//        try {
//            PrototypeUI proto = (PrototypeUI) t.getTransferData(Constants.PROTOTYPE_DATA_FLAVOR);
//            if (point.x < view.getConnectableWidget().getWidgetWidth() / 2) {
//                view.getCapsuleModel().getTaskModel().addPrototype(proto, IOType.INPUT);
//            } else {
//                view.getCapsuleModel().getTaskModel().addPrototype(proto, IOType.OUTPUT);
//            }
//        } catch (UnsupportedFlavorException ex) {
//            MoleExceptionManagement.showException(ex);
//        } catch (IOException ex) {
//            MoleExceptionManagement.showException(ex);
//        }
//    }
//}