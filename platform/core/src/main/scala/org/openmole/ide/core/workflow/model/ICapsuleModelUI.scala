/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.palette.PaletteElementFactory

trait ICapsuleModelUI {
  def startingCapsule: Boolean
  
  def containsTask: Boolean

  def dataProxy: Option[PaletteElementFactory]
  
  def setDataProxy(pef: PaletteElementFactory)
  
  def defineStartingCapsule(on: Boolean)
  
  def addInputSlot
  
  def nbInputSlots: Int
  
  def removeInputSlot
}