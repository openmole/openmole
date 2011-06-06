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

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.commons.CapsuleType._
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.palette.PaletteElementFactory

class CapsuleModelUI(var dataProxy: Option[PaletteElementFactory] = None, var nbInputSlots: Int = 0) extends ICapsuleModelUI{

  val category= "Task Tapsules"
  var startingCapsule = false
 // var containsTask = false
 var capsuleType = CAPSULE
  
  //def this(pef: PaletteElementFactory)= this(Some(pef))
 
  def addInputSlot= nbInputSlots+= 1
  
  def removeInputSlot= nbInputSlots-= 1
  
  def setDataProxy(pef: PaletteElementFactory)={
    dataProxy= Some(pef)
    if (ElementFactories.isExplorationTaskFactory(pef.panelUIData)) capsuleType = EXPLORATION_TASK else capsuleType = BASIC_TASK
   // containsTask= true
  }
  
  override def defineStartingCapsule(on: Boolean)= {
    startingCapsule= on
    if (on) nbInputSlots= 1
  }
}