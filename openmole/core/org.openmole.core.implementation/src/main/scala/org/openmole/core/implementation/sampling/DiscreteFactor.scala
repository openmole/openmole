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

package org.openmole.core.implementation.sampling

import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IIterable
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.sampling.IDiscreteFactor

object DiscreteFactor {
  
  implicit def iterableFactorConversion[T, D <: IDomain[T] with IIterable[T]](f: Factor[T,D]) = this(f)
  
  implicit def iterableFactorDecorator[T, D <: IDomain[T] with IIterable[T]](f: Factor[T,D]) = new {
    def build(context: IContext): Iterator[Iterable[IVariable[T]]] = 
      iterableFactorConversion(f).build(context)
  }
  
  def apply[T, D <: IDomain[T] with IIterable[T]](f: Factor[T, D]) = new DiscreteFactor(f.prototype, f.domain)
   
}


class DiscreteFactor[T, +D <: IDomain[T] with IIterable[T]](
  prototype: IPrototype[T],
  domain: D) extends Factor[T, D](prototype, domain) with IDiscreteFactor[T, D] with Sampling {
  
  override def prototypes = List(prototype)
  
  override def build(context: IContext): Iterator[Iterable[IVariable[T]]] = 
    domain.iterator(context).map{v => List(new Variable(prototype, v))}
  
}
