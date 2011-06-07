/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.CapsuleType
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.implementation.paint.ConnectableWidget

trait ICapsuleView {
  def capsuleType: CapsuleType.Value
  
  def dataProxy: Option[PaletteElementFactory]
  
  def startingCapsule: Boolean
  
  def scene: MoleScene
  
  def connectableWidget: ConnectableWidget
  
  def encapsule(pef: PaletteElementFactory)

  def addInputSlot(startingCapsule: Boolean): ISlotWidget
  
  def nbInputSlots: Int
  
  def setDataProxy(pef: PaletteElementFactory)
}

//trait ICapsuleModelUI {
//  def startingCapsule: Boolean
//  
//  def capsuleType: CapsuleType.Value
//
//  def dataProxy: Option[PaletteElementFactory]
//  
//  def setDataProxy(pef: PaletteElementFactory)
//  
//  def defineStartingCapsule(on: Boolean)
//  
//  def addInputSlot
//  
//  def nbInputSlots: Int
//  
//  def removeInputSlot
//}
