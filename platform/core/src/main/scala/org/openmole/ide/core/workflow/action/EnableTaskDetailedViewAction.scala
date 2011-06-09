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

package org.openmole.ide.core.workflow.action

import scala.collection.JavaConversions._
import javax.swing.AbstractButton
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import org.openmole.ide.core.control.MoleScenesManager

class EnableTaskDetailedViewAction extends ActionListener{
  
  override def actionPerformed(ae: ActionEvent)= {
    MoleScenesManager.detailedView= ae.getSource.asInstanceOf[AbstractButton].isSelected
    MoleScenesManager.moleScenes.foreach(s=> {s.manager.capsules.values.foreach(_.connectableWidget.setDetailedView);s.validate;s.refresh})
  }
}

//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Iterator;
//import javax.swing.AbstractButton;
//import org.openmole.ide.core.control.MoleScenesManager;
//import org.openmole.ide.core.workflow.implementation.MoleScene;
//import org.openmole.ide.core.workflow.model.ICapsuleUI;
//import org.openmole.ide.core.workflow.model.IMoleScene;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class EnableTaskDetailedView implements ActionListener {
//
//    @Override
//    public void actionPerformed(ActionEvent ae) {
//        AbstractButton button = (AbstractButton) ae.getSource();
//        MoleScenesManager.getInstance().setDetailedView(button.isSelected());
//        for (Iterator<IMoleScene> its = MoleScenesManager.getInstance().getMoleScenes().iterator(); its.hasNext();) {
//            MoleScene scene = (MoleScene) its.next();
//            for (ICapsuleUI cv : scene.getManager().getCapsuleUIs()) {
//                cv.getConnectableWidget().setDetailedView();
//            }
//            scene.validate();
//            scene.refresh();
//        }
//    }
//}