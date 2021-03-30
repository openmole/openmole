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

package org.openmole.plugin.sampling.lhs

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object LHS {

  def apply(sample: FromContext[Int], factor: Seq[ScalarOrSequenceOfDouble[_]]) =
    Sampling { p ⇒
      import p._
      val s = sample.from(context)
      val vectorSize = factor.map(_.size(context)).sum
      def values = LHS.lhsValues(vectorSize, s, random())
      values.map(v ⇒ ScalarOrSequenceOfDouble.unflatten(factor, v).from(context)).iterator
    } withValidate { sample.validate } inputs { factor.flatMap(_.inputs) } prototypes { factor.map(_.prototype) }

  def lhsValues(dimensions: Int, samples: Int, rng: scala.util.Random) = Array.fill(dimensions) {
    org.openmole.tool.random.shuffled(0 until samples)(rng).map { i ⇒ (i + rng.nextDouble) / samples }.toArray
  }.transpose

}

