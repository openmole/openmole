/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.sampling._

object SampleSampling {

  def apply(sampling: Sampling, size: FromContext[Int]) =
    new SampleSampling(sampling, size)

}

sealed class SampleSampling(val sampling: Sampling, val size: FromContext[Int]) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes

  override def apply() = FromContext.apply { (context, rng) ⇒
    val sampled = sampling().from(context)(rng).toVector
    val sampledSize = sampled.size
    val s = size.from(context)(rng)
    Iterator.continually(rng().nextInt(sampledSize)).take(s).map(i ⇒ sampled(i))
  }

}