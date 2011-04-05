/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.ui.ide.workflow.implementation.MoleScene
import org.openmole.ui.ide.workflow.implementation.TaskUI
import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget
import org.openmole.ui.ide.workflow.implementation.paint.ConnectableWidget

trait ICapsuleView {
  def capsuleModel: ICapsuleModelUI
  
  def scene: MoleScene
  
  def connectableWidget: ConnectableWidget
  
  def encapsule(taskUI: TaskUI)

  def addInputSlot: ISlotWidget
  
 // def changeConnectableWidget
}

//interface ICapsuleView {
//    void encapsule(TaskUI taskUI) throws UserBadDataError;
//    ICapsuleModelUI<IGenericCapsule> getCapsuleModel();
//    ISlotWidget addInputSlot();
//    ConnectableWidget getConnectableWidget();
//    void changeConnectableWidget();
//    IMoleScene getMoleScene();
//}