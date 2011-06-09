/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.CapsuleType
import org.openmole.ide.core.properties.ITaskDataUI
import org.openmole.ide.core.palette.DataProxyUI
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.implementation.paint.ConnectableWidget

trait ICapsuleUI {
  def capsuleType: CapsuleType.Value
  
  def dataProxy: Option[DataProxyUI[ITaskDataUI]]
  
  def startingCapsule: Boolean
  
  def scene: MoleScene
  
  def connectableWidget: ConnectableWidget
  
  def encapsule(dpu: DataProxyUI[ITaskDataUI])

  def addInputSlot(startingCapsule: Boolean): ISlotWidget
  
  def nbInputSlots: Int
  
  def setDataProxy(dpu: DataProxyUI[ITaskDataUI])
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
