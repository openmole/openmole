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
 * The parameter is a variable wich is injected in the data flow durring the
 * workflow execution just before the begining of a task execution. It can be
 * usefull for testing purposes and for defining default value of inputs of a
 * task.
 *
 */
trait IParameter[T] {

  /**
   * Get the variable which is injected.
   *
   * @return the variable
   */
  def variable: IVariable[T]

  /**
   * Get if an existing value in the context should be overriden. If override
   * is true the if a value with the same name is allready presentin the
   * context when the parameter is injected the vaule will be discarded.
   *
   * @return true if an existing value should be overriden false otherwise
   */
  def `override`: Boolean
}
