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

  def apply(p: Traversable[IParameter[_]]): ParameterSet =
    new ParameterSet {
      val parameters = p.toIterable
    }

  def apply(p: IParameter[_]*): ParameterSet =
    ParameterSet(p)

}

trait ParameterSet extends Set[IParameter[_]] with SetLike[IParameter[_], ParameterSet] { self ⇒

  def parameters: Iterable[IParameter[_]]

  @transient lazy val parameterMap =
    TreeMap.empty[String, IParameter[_]] ++ parameters.map { p ⇒ (p.variable.prototype.name, p) }

  override def empty = ParameterSet.empty
  override def iterator: Iterator[IParameter[_]] = parameterMap.values.iterator

  def +(p: IParameter[_]) = ParameterSet(p :: parameters.toList)

  def -(p: IParameter[_]) = ParameterSet((parameterMap - p.variable.prototype.name).values.toList)

  def contains(p: IParameter[_]) = parameterMap.contains(p.variable.prototype.name)

}
