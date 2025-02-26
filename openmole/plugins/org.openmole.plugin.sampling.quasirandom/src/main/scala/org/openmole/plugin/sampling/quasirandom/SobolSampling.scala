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
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue

object SobolSampling:

  def sobolValues(dimension: Int, samples: Int): Iterator[Seq[Double]] =
    val sequence = new SobolSequenceGenerator(dimension)
    for
      v ← Iterator.continually(sequence.nextVector()).slice(1, 1 + samples)
    yield v.toSeq

  given IsSampling[SobolSampling] = sobol =>
    def apply = FromContext: p =>
      import p._
      sobolValues(sobol.factor.size, sobol.sample.from(context)).map { ScalableValue.toVariables(sobol.factor, _)(context) }

    Sampling(
      apply,
      sobol.factor.map(_.prototype),
      sobol.factor.flatMap(_.inputs),
      sobol.sample.validate
    )


case class SobolSampling(sample: FromContext[Int], factor: Seq[ScalableValue])
