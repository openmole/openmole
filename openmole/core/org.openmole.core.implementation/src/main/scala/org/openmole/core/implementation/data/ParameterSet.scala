/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.data

import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameter
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import scala.collection.immutable.TreeMap

object ParameterSet {
  val empty = new ParameterSet(List.empty)

  def apply(parameters: (IPrototype[T], T) forSome { type T }*) =
    new ParameterSet(parameters.toList.map { case (p, v) ⇒ new Parameter(p, v) })
}

class ParameterSet(parameters: List[IParameter[_]]) extends Set[IParameter[_]] with IParameterSet {

  @transient private lazy val _parameters =
    TreeMap.empty[String, IParameter[_]] ++ parameters.map { p ⇒ (p.variable.prototype.name, p) }

  override def empty = ParameterSet.empty
  override def iterator: Iterator[IParameter[_]] = _parameters.values.iterator

  override def +(p: IParameter[_]) = new ParameterSet(p :: parameters)
  override def -(p: IParameter[_]) = new ParameterSet((_parameters - p.variable.prototype.name).values.toList)
  override def contains(p: IParameter[_]) = _parameters.contains(p.variable.prototype.name)

  override def +[T](prototype: IPrototype[T], v: T, `override`: Boolean = false): ParameterSet =
    this + new Parameter(prototype, v, `override`)

}
