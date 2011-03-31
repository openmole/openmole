
package org.openmole.ui.ide.workflow.implementation


import java.awt.Point
import org.openmole.misc.tools.obj.Instanciator
import org.openmole.ui.ide.workflow.model.ICapsuleView
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI
import org.openmole.ui.ide.palette.MoleConcepts

object UIFactory {

  def createTaskModelInstance(modelClass: Class[_<:IGenericTaskModelUI[_]]): IGenericTaskModelUI[_] = {
    Instanciator.instanciate(modelClass)
  }
  
  def createCapsule(scene: MoleScene, locationPoint: Point): ICapsuleView = {
    val obUI = new CapsuleViewUI(scene,new CapsuleModelUI(),Preferences.getProperties(MoleConcepts.CAPSULE_INSTANCE,
                    classOf[org.openmole.core.implementation.capsule.Capsule]))
    scene.initCapsuleAdd(obUI)
    scene.manager.registerCapsuleView(obUI)
    scene.addNode(scene.manager.getNodeID).setPreferredLocation(locationPoint)
    obUI    
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
}