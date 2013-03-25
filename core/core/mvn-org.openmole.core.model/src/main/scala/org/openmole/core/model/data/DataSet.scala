/*
 * Copyright (C) 2010 Romain Reuillon
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
 * It is a set of @link{Data}. It allows manipulating data by set instead of
 * individualy.
 */
import scala.collection.SetLike
import scala.collection.immutable.TreeMap

object DataSet {

  val empty = DataSet(List.empty)

  def apply(d: Traversable[Data[_]]): DataSet =
    new DataSet {
      val data = d.toIterable
    }

  def apply(d: Data[_]*): DataSet = DataSet(d)
}

trait DataSet extends Set[Data[_]] with SetLike[Data[_], DataSet] { self ⇒

  def data: Iterable[Data[_]]

  @transient lazy val dataMap: Map[String, Data[_]] =
    TreeMap.empty[String, Data[_]] ++ data.map { d ⇒ (d.prototype.name, d) }

  /**
   * Get the @link{Data} by its name as an Option.
   *
   * @param name the name of the @link{Data}
   * @return Some(data) if it is present in the data set None otherwise
   */
  def apply(name: String): Option[Data[_]] = dataMap.get(name)

  /**
   * Test if a variable with a given name is present in the data set.
   *
   * @param name the name of the @link{Data}
   * @return true if the variable with a matching name is present in the data
   * set false otherwise
   */
  def contains(name: String): Boolean = dataMap.contains(name)

  override def empty = DataSet.empty

  override def iterator: Iterator[Data[_]] = dataMap.values.iterator

  def ++(d: Traversable[Data[_]]) = DataSet(d.toList ::: data.toList)

  def +(set: DataSet): DataSet = DataSet(set.toList ::: data.toList)

  def +(d: Data[_]) = DataSet(d :: data.toList)

  def -(d: Data[_]) = DataSet((dataMap - d.prototype.name).map { _._2 }.toList)

  override def contains(data: Data[_]) = dataMap.contains(data.prototype.name)

  def toMap = map(d ⇒ d.prototype.name -> d).toMap[String, Data[_]]

}
