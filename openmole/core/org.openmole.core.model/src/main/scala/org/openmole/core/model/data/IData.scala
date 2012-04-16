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
 * It modelizes atomic elements of data-flows. IData is a typed data chunk with
 * meta-information. Tasks takes IData in input and produce IData in output. 
 * IData travels through transitions and data channels.
 *
 */
trait IData[T] {

  /**
   * The mode is meta-information on the IData. It indicates the manner a task
   * uses it. For a list of modalties see {@see DataModeMask}.
   *
   * @return mode of the data
   */
  def mode: IDataMode

  /**
   * Data chunks are named and typed. Get the prototype (type and name) of 
   * this data chunk.
   *
   * @return the prototype of the IData
   */
  def prototype: IPrototype[T]

}
