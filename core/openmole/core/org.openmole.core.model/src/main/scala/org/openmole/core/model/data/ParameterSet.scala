/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.model.data

import scala.collection.SetLike
import scala.collection.immutable.TreeMap

object ParameterSet {
  val empty = ParameterSet(Iterable.empty)

  def apply(p: Traversable[Parameter[_]]): ParameterSet =
    new ParameterSet {
      val parameters = p.toIterable
    }

  def apply(p: Parameter[_]*): ParameterSet =
    ParameterSet(p)

}

trait ParameterSet extends Set[Parameter[_]] with SetLike[Parameter[_], ParameterSet] { self ⇒

  def parameters: Iterable[Parameter[_]]

  @transient lazy val parameterMap =
    TreeMap.empty[String, Parameter[_]] ++ parameters.map { p ⇒ (p.variable.prototype.name, p) }

  override def empty = ParameterSet.empty
  override def iterator: Iterator[Parameter[_]] = parameterMap.values.iterator

  def +(p: Parameter[_]) = ParameterSet(p :: parameters.toList)

  def -(p: Parameter[_]) = ParameterSet((parameterMap - p.variable.prototype.name).values.toList)

  def contains(p: Parameter[_]) = parameterMap.contains(p.variable.prototype.name)

}
