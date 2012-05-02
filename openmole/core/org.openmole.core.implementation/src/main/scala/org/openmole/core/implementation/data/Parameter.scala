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

import org.openmole.core.model.data.{ IParameter, IVariable, IPrototype }

object Parameter {
  implicit lazy val parameterOrderingOnName = new Ordering[IParameter[_]] {
    override def compare(left: IParameter[_], right: IParameter[_]) =
      Prototype.prototypeOrderingOnName.compare(left.variable.prototype, right.variable.prototype)
  }

  implicit def tuple2IterableToParameters(values: Iterable[(IPrototype[T], T) forSome { type T }]) = values.map { case (p, v) ⇒ new Parameter(p, v) }
}

class Parameter[T](val variable: IVariable[T], val `override`: Boolean) extends IParameter[T] {

  def this(prototype: IPrototype[T], value: T, `override`: Boolean) = this(new Variable(prototype, value), `override`)

  def this(prototype: IPrototype[T], value: T) = this(prototype, value, false)

  def this(variable: IVariable[T]) = this(variable, false)

}
