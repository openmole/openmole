/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.control

import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.model.IMoleScene

object TabsManager {

  def getCurrentObject: Object= MoleScenesManager.getCurrentObject.getOrElse(TaskSettingsManager.getCurrentObject)

  def getCurrentScene: IMoleScene= {
    val o= getCurrentObject
    o match{
      case x: CapsuleViewUI=> x.scene
      case _=> o.asInstanceOf[IMoleScene]
    }
  }
  
  def removeCurrentSceneAndChild= MoleScenesManager.removeCurrentSceneAndChilds(getCurrentScene)
}

//public class TabsManager {
//
//    private static TabsManager instance = null;
//
//    private Object getCurrentObject() {
//        Object ob = MoleScenesManager.getInstance().getCurrentObject();
//        if (ob == null) {
//            return TaskSettingsManager.getInstance().getCurrentObject();
//        }
//        return ob;
//    }
//
//    private IMoleScene getCurrentScene() {
//        Object o = getCurrentObject();
//        if (o.getClass().equals(CapsuleViewUI.class)) {
//            return ((ICapsuleView) o).getMoleScene();
//        }
//        return (MoleScene) o;
//    }
//
//    public void removeCurrentSceneAndChild() {
//        MoleScenesManager.getInstance().removeCurrentSceneAndChilds(getCurrentScene());
//    }
//
//    public static TabsManager getInstance() {
//        if (instance == null) {
//            instance = new TabsManager();
//        }
//        return instance;
//    }
//}
