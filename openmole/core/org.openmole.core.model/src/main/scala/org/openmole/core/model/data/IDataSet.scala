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
 * It is a set of @link{IData}. It allows manipulating data by set instead of
 * individualy.
 */
import scala.collection.SetLike

trait IDataSet extends SetLike[IData[_], IDataSet with Set[IData[_]]] {
  
  //override def empty: this.type

  /**
   * Get the @link{IData} by its name as an Option.
   * 
   * @param name the name of the @link{IData}
   * @return Some(data) if it is present in the data set None otherwise
   */
  def apply(name: String): Option[IData[_]]
  
  /**
   * Test if a variable with a given name is present in the data set.
   * 
   * @param name the name of the @link{IData}
   * @return true if the variable with a matching name is present in the data
   * set false otherwise
   */
  def contains(name: String): Boolean
  
  def +(p: IPrototype[_]): IDataSet
  def ++(p: Traversable[IPrototype[_]]): IDataSet
  
}
