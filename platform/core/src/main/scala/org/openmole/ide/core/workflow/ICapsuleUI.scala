/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow

import org.openmole.ide.core.commons.CapsuleType
import org.openmole.ide.core.data.ITaskDataUI
import org.openmole.ide.core.dataproxy._

trait ICapsuleUI {
  def capsuleType: CapsuleType.Value
  
  def dataProxy: Option[TaskDataProxyUI]
  
  def startingCapsule: Boolean
  
  def scene: MoleScene
  
  def connectableWidget: ConnectableWidget
  
  def encapsule(dpu: TaskDataProxyUI)

  def addInputSlot(startingCapsule: Boolean): ISlotWidget
  
  def nbInputSlots: Int
  
  def setDataProxy(dpu: TaskDataProxyUI)
}

//trait ICapsuleModelUI {
//  def startingCapsule: Boolean
//  
//  def capsuleType: CapsuleType.Value
//
//  def dataProxy: Option[DataProxyUI]
//  
//  def setDataProxy(dpu: DataProxyUI)
//  
//  def defineStartingCapsule(on: Boolean)
//  
//  def addInputSlot
//  
//  def nbInputSlots: Int
//  
//  def removeInputSlot
//}
