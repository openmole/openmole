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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.{ IData, IDataSet, IPrototype }
import scala.collection.Iterator
import scala.collection.immutable.TreeMap

object DataSet {
  val empty = new DataSet(List.empty)

  def apply(prototypes: IPrototype[_]*): DataSet = apply(prototypes.toIterable)
  def apply(prototypes: Traversable[IPrototype[_]]): DataSet = new DataSet(prototypes.map { new Data(_) }.toList)
}

class DataSet(data: List[IData[_]]) extends Set[IData[_]] with IDataSet {

  @transient private lazy val _data =
    TreeMap.empty[String, IData[_]] ++ data.map { d ⇒ (d.prototype.name, d) }

  override def empty = DataSet.empty
  override def iterator: Iterator[IData[_]] = _data.values.iterator
  override def apply(name: String) = _data.get(name)
  override def contains(name: String): Boolean = _data.contains(name)

  override def ++(d: Traversable[IData[_]]) = new DataSet(d.toList ::: data)
  override def +(set: IDataSet): IDataSet = new DataSet(set.toList ::: data)
  override def +(p: IPrototype[_]): IDataSet = this + new Data(p)
  override def +(data: IData[_]) = new DataSet(data :: this.data)
  override def -(data: IData[_]) = new DataSet((_data - (data.prototype.name)).map { _._2 }.toList)
  override def contains(data: IData[_]) = _data.contains(data.prototype.name)

}
