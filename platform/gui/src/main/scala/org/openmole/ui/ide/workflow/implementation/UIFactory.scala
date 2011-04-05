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
package org.openmole.ui.ide.workflow.implementation


import java.awt.Point
import org.openmole.misc.tools.obj.Instanciator
import org.openmole.ui.ide.workflow.model.ICapsuleView
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI
import org.openmole.ui.ide.palette.MoleConcepts

object UIFactory {

  def createTaskModelInstance(modelClass: Class[_ <: GenericTaskModelUI]) = Instanciator.instanciate(modelClass)
  
  def createCapsule(scene: MoleScene, locationPoint: Point): ICapsuleView = {
    val obUI = new CapsuleViewUI(scene,new CapsuleModelUI(),Preferences.properties(MoleConcepts.CAPSULE_INSTANCE,
                    classOf[org.openmole.core.implementation.capsule.Capsule]))
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleView(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI    
  }
}
//   public ICapsuleView createCapsule(MoleScene scene,
//            Point locationPoint) {
//        ICapsuleView obUI = null;
//        try {
//            obUI = new CapsuleViewUI(scene,
//                    new CapsuleModelUI(),
//                    Preferences.getInstance().getProperties(CategoryName.CAPSULE_INSTANCE,
//                    org.openmole.core.implementation.capsule.Capsule.class));
//        } catch (UserBadDataError ex) {
//            MoleExceptionManagement.showException(ex);
//        }
//
//        scene.initCapsuleAdd(obUI);
//        scene.getManager().registerCapsuleView(obUI);
//        scene.addNode(scene.getManager().getNodeID()).setPreferredLocation(locationPoint);
//        return obUI;
//    }
