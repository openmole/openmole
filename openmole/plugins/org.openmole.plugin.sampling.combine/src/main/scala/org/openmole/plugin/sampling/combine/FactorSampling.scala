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

package org.openmole.plugin.sampling.combine

import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.sampling.ISampling

class FactorSampling(factor: IFactor[T,IDomain[T]] forSome{type T}) extends ISampling {
 
  override def build(context: IContext): Iterable[Iterable[IVariable[_]]] = {
    typedBuild(context, factor)
  }
  
  private def typedBuild[T](context: IContext, factor: IFactor[T,IDomain[T]]): Iterable[Iterable[IVariable[T]]] = {
    return new Iterable[Iterable[IVariable[T]]] {

      override def iterator: Iterator[Iterable[IVariable[T]]] = {
        new Iterator[Iterable[IVariable[T]]] {

          val iterator = factor.domain.iterator(context)

          override def hasNext: Boolean = iterator.hasNext
                    
          override def next: Iterable[IVariable[T]] = {
            val v = new Variable(factor.prototype, iterator.next)
            List(v) 
          }
        }
      }
    }
  }
  
}
