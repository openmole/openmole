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

package org.openmole.core.implementation.sampling

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.sampling._

object DiscreteFactor {

  implicit def iterableFactorConversion[T, D <: IDomain[T] with IIterable[T]](f: Factor[T, D]) = this(f)

  implicit def iterableFactorDecorator[T, D <: IDomain[T] with IIterable[T]](f: Factor[T, D]) = new {
    def build(context: Context): Iterator[Iterable[Variable[T]]] =
      iterableFactorConversion(f).build(context)
  }

  def apply[T, D <: IDomain[T] with IIterable[T]](f: IFactor[T, D]) = new DiscreteFactor(f.prototype, f.domain)

}

class DiscreteFactor[T, +D <: IDomain[T] with IIterable[T]](
    prototype: Prototype[T],
    domain: D) extends Factor[T, D](prototype, domain) with IDiscreteFactor[T, D] with Sampling {

  override def prototypes = List(prototype)

  override def build(context: Context): Iterator[Iterable[Variable[T]]] =
    domain.iterator(context).map { v ⇒ List(Variable(prototype, v)) }

}
