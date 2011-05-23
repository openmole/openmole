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

import org.openmole.ide.core.workflow.model.ICapsuleModelUI

class CapsuleModelUI(var taskUI: Option[TaskUI] = None, var nbInputSlots: Int = 0) extends ICapsuleModelUI{

  val category= "Task Tapsules"
  var startingCapsule = false
  var containsTask = false
  
  def this(taskUI: TaskUI)= this(Some(taskUI))
 
  def addInputSlot= nbInputSlots+= 1
  
  def removeInputSlot= nbInputSlots-= 1
  
  def setTaskUI(tUI: TaskUI)={
    taskUI= Some(tUI)
    containsTask= true
  }
  
  override def defineStartingCapsule(on: Boolean)= {
    startingCapsule= on
    if (on) nbInputSlots= 1
  }
}