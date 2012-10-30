/*
 * Copyright (C) 2011 Romain Reuillon
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

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.sampling._
import org.openmole.plugin.domain.modifier._

sealed class ReplicationSampling[T](sampling: Sampling, seederFactor: Factor[T, Domain[T] with Discrete[T]], nbReplication: Int) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = seederFactor.prototype :: sampling.prototypes.toList

  override def build(context: Context): Iterator[Iterable[Variable[_]]] =
    new CompleteSampling(sampling, Factor(seederFactor.prototype, seederFactor.domain.take(nbReplication))).build(context)

}
