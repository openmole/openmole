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

package org.openmole.core.model.data

/**
 * This class is a collection of masks to set modalties on how a task uses
 * {@see IData}.
 */
object DataModeMask {

  /**
   * IData is optionnal. If this data is no found at the begining of a task 
   * execution no error will be raised and the task should accomodate of it.
   *
   */
  val optional = new DataModeMask(0x0001)

  /**
   * The value corresponding to the IData is mutable. The task will modify the
   * value, theyrefore the workflow system will clone the value when needed
   * to ensure consistency. This data mode only applies for task inputs.
   *
   */
  val mutable = new DataModeMask(0x0002)

  /**
   * The value corresponding to the IData is used for system level information
   * (as oposed to buisiness or user level information). This mask is use by the
   * workflow system.
   *
   */
  val system = new DataModeMask(0x0004)
  
  
  /**
   * The value corresponding to the IData can be used by an exploration 
   * transition as a exploration set. This should be set only on array.
   *
   */
  val explore = new DataModeMask(0x0008)
}

class DataModeMask(val value: Int)