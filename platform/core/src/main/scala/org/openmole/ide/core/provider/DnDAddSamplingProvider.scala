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
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.commons.ApplicationCustomize

class DnDAddSamplingProvider(molescene: MoleScene) extends DnDProvider(molescene){ 

  override def  isAcceptable(widget: Widget,point: Point,transferable: Transferable)= {
    var state = ConnectorState.REJECT
    if (transferable.isDataFlavorSupported(ApplicationCustomize.SAMPLING_DATA_FLAVOR)) {
      state= ConnectorState.ACCEPT
    }
    state
  }
 
  override def accept(widget: Widget,point: Point,t: Transferable) = System.out.println("ACCEPT")
}
//import java.awt.Point;
//import java.awt.datatransfer.Transferable;
//import org.netbeans.api.visual.action.ConnectorState;
//import org.netbeans.api.visual.widget.Widget;
//import org.openmole.ide.core.commons.ApplicationCustomize;
//import org.openmole.ide.core.workflow.implementation.MoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
//public class DnDAddSamplingProvider extends DnDProvider {
//
//    public DnDAddSamplingProvider(MoleScene molescene) {
//        super(molescene);
//        System.out.println("DnDAddSamplingProvider constructor");
//    }
//
//    @Override
//    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
//
//        System.out.println("isAcceptable :: " + widget);
//        ConnectorState state = ConnectorState.REJECT;
//        if (transferable.isDataFlavorSupported(ApplicationCustomize.SAMPLING_DATA_FLAVOR)) {
//            state = ConnectorState.ACCEPT;
//        }
//        return state;
//    }
//
//    @Override
//    public void accept(Widget widget, Point point, Transferable t) {
//        System.out.println("ACCEPT");
//    }
//}