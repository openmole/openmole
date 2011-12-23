/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.sampling.combine

import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IInfiniteDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.core.model.sampling.ISampling

class ReplicationSampling[T](sampling: ISampling, seederFactor: IFactor[T, IInfiniteDomain[T]], nbReplication: Int) extends ISampling {

  override def prototypes = seederFactor.prototype :: sampling.prototypes.toList
  
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    val factorIts = seederFactor.domain.iterator(context).sliding(nbReplication, nbReplication)

    (for(sample <- sampling.build(context).toIterable) yield sample -> factorIts.next).flatMap {
      case(sample, factorIt) => factorIt.map(rep => sample ++ List(new Variable(seederFactor.prototype, rep)))
    }.iterator
  }
}
