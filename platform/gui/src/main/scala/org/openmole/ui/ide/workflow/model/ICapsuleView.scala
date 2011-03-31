/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.ui.ide.workflow.implementation.MoleScene
import org.openmole.ui.ide.workflow.implementation.TaskUI
import org.openmole.ui.ide.workflow.implementation.paint.ConnectableWidget

trait ICapsuleView {
  def capsuleModel[T<: IGenericCapsule]: ICapsuleModelUI[T]
  
  def scene: MoleScene
  
  def connectbaleWidget: ConnectableWidget
  
  def encapsule(taskUI: TaskUI)

  def addInputSlot
  
  def changeConnectableWidget
}

//interface ICapsuleView {
//    void encapsule(TaskUI taskUI) throws UserBadDataError;
//    ICapsuleModelUI<IGenericCapsule> getCapsuleModel();
//    ISlotWidget addInputSlot();
//    ConnectableWidget getConnectableWidget();
//    void changeConnectableWidget();
//    IMoleScene getMoleScene();
//}