/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.model.task

/**
 *
 * The mole task is a task that executes a mole.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
import org.openmole.core.model.data.DataModeMask
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.mole.IMole

trait IMoleTask extends ITask {

  /**
   *
   * Get the mole executed by this task.
   *
   * @return the mole executed by this task
   */
  def mole: IMole
    
  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_]): this.type

  def addOutput(capsule: IGenericCapsule, data: IData[_]): this.type
  
  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_],masks: Array[DataModeMask]): this.type
 
  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_], forceArray: Boolean): this.type

  def addOutput(capsule: IGenericCapsule, data: IData[_], forceArray: Boolean): this.type
  
  def addOutput(capsule: IGenericCapsule, prototype: IPrototype[_],masks: Array[DataModeMask], forceArray: Boolean): this.type
}
