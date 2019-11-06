/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.sampling.quasirandom

import org.apache.commons.math3.random.SobolSequenceGenerator
import org.openmole.core.dsl.extension._

object SobolSampling {

  def apply(samples: FromContext[Int], factors: ScalarOrSequenceOfDouble[_]*) =
    Sampling { p ⇒
      import p._
      SobolSampling.sobolValues(factors.size, samples.from(context)).map { ScalarOrSequenceOfDouble.unflatten(factors, _)(context) }
    } validate { samples } inputs { factors.flatMap(_.inputs) } prototypes { factors.map(_.prototype) }

  def sobolValues(dimension: Int, samples: Int) = {
    val sequence = new SobolSequenceGenerator(dimension)
    for {
      v ← Iterator.continually(sequence.nextVector()).take(samples)
    } yield v.toSeq
  }

}
