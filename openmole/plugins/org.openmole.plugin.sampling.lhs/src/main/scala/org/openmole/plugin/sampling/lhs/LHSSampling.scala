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

package org.openmole.plugin.sampling.lhs

import org.openmole.core.model.sampling.ISampling
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.service.Random._
import java.util.Random
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.sampling.IFactor
import org.openmole.misc.workspace.Workspace

sealed class LHSSampling(samples: Int, rng: Random, factors: IFactor[Double, IDomain[Double] with IBounded[Double]]*) extends Sampling {

  def this(samples: Int, factors: IFactor[Double, IDomain[Double] with IBounded[Double]]*) = this(samples, Workspace.newRNG, factors: _*)

  override def prototypes = factors.map{_.prototype}
  
  override def build(context: IContext): Iterator[Iterable[IVariable[Double]]] = 
    (0 until samples).map {
      _ =>
      (0 until factors.size).map {
        i => (i + rng.nextDouble) / factors.size
      }.shuffled(rng).zip(factors).map {
        case (v, f) => new Variable(f.prototype, v.scale(f.domain.min(context), f.domain.max(context)))
      }
    }.toIterator
  
}
