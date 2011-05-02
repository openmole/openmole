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
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.UIFactory
import org.openmole.ide.core.commons.ApplicationCustomize
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.workflow.implementation.TaskUI



class DnDNewTaskProvider(molescene: MoleScene) extends DnDProvider(molescene) {

  override def isAcceptable(widget: Widget, point: Point,transferable: Transferable)= ConnectorState.ACCEPT
 
  override def accept(widget: Widget,point: Point,transferable: Transferable)= {
    println("+Accept")
   
    val capsuleView = UIFactory.createCapsule(molescene,point)
    capsuleView.addInputSlot
  //  capsuleView.encapsule(transferable.getTransferData(ApplicationCustomize.TASK_DATA_FLAVOR).asInstanceOf[ITaskFactoryUI].buildEntity)
    capsuleView.encapsule(transferable.getTransferData(ApplicationCustomize.TASK_DATA_FLAVOR).asInstanceOf[PaletteElementFactory].buildEntity.asInstanceOf[TaskUI])
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
//import org.openmole.ide.core.commons.ApplicationCustomize;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.implementation.UIFactory;
//import org.openmole.ide.core.workflow.implementation.MoleScene;
//import org.openmole.ide.core.workflow.implementation.CapsuleViewUI;
//import org.openmole.ide.core.workflow.implementation.TaskUI;
//import org.openmole.ide.core.workflow.model.ICapsuleView;
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