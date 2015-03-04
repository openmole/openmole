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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.FromContext

import util.Random

object SampleSampling {

  def apply(sampling: Sampling, size: FromContext[Int]) =
    new SampleSampling(sampling, size)

}

sealed class SampleSampling(val sampling: Sampling, val size: FromContext[Int]) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes

  override def build(context: Context)(implicit rng: Random): Iterator[Iterable[Variable[_]]] = {
    val sampled = sampling.build(context).toVector
    val sampledSize = sampled.size
    val s = size.from(context)
    Iterator.continually(rng.nextInt(sampledSize)).take(s).map(i ⇒ sampled(i))
  }

}