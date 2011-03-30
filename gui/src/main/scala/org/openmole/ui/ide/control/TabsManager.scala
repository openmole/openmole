/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.control


import org.openmole.ui.ide.control.TaskSettingsManager

object TabsManager {

  def getCurrentObject= {
    MoleScenesManager.getCurrentObject.getOrElse(TaskSettingsManager.getCurrentObject.get)
  }
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
