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

package org.openmole.ui.ide.provider

import java.awt.Point
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.widget.Widget
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ui.ide.workflow.implementation.MoleScene
import org.openmole.ui.ide.workflow.implementation.UIFactory
import org.openmole.ui.ide.commons.ApplicationCustomize

class DnDNewTaskProvider(molescene: MoleScene,var capsuleView: Option[CapsuleViewUI[_]]= None) extends DnDProvider(molescene) {

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= ConnectorState.ACCEPT
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {
    capsuleView= Some(UIFactory.createCapsule(molescene, point))
    capsuleView.get.encapsule(transferable.getTransferData(ApplicationCustomize.TASK_DATA_FLAVOR).asInstanceOf[TaskUI])
    molescene.repaint
    molescene.revalidate
  }
}
  
//import java.awt.Point;
//import java.awt.datatransfer.Transferable;
//import java.awt.datatransfer.UnsupportedFlavorException;
//import java.io.IOException;
//import org.netbeans.api.visual.action.ConnectorState;
//import org.netbeans.api.visual.widget.Widget;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ui.ide.commons.ApplicationCustomize;
//import org.openmole.ui.ide.exception.MoleExceptionManagement;
//import org.openmole.ui.ide.implementation.UIFactory;
//import org.openmole.ui.ide.workflow.implementation.MoleScene;
//import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
//import org.openmole.ui.ide.workflow.implementation.TaskUI;
//import org.openmole.ui.ide.workflow.model.ICapsuleView;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class DnDNewTaskProvider extends DnDProvider {
//
//    private ICapsuleView capsuleView;
//
//    public DnDNewTaskProvider(MoleScene molescene,
//            CapsuleViewUI cv) {
//        super(molescene);
//        this.capsuleView = cv;
//    }
//
//    public DnDNewTaskProvider(MoleScene molescene) {
//        super(molescene);
//        this.capsuleView = null;
//    }
//
//    @Override
//    public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
//        return ConnectorState.ACCEPT;
//    }
//
//    @Override
//    public void accept(Widget widget, Point point, Transferable transferable) {
//        try {
//            if (capsuleView == null) {
//                capsuleView = UIFactory.createCapsule(scene, point);
//                capsuleView.addInputSlot();
//            }
//            capsuleView.encapsule((TaskUI) transferable.getTransferData(ApplicationCustomize.TASK_DATA_FLAVOR));
//        } catch (UnsupportedFlavorException ex) {
//            MoleExceptionManagement.showException(ex);
//        } catch (IOException ex) {
//            MoleExceptionManagement.showException(ex);
//        } catch (UserBadDataError ex) {
//            MoleExceptionManagement.showException(ex);
//        } finally {
//            scene.repaint();
//            scene.revalidate();
//        }
//
//    }
//}