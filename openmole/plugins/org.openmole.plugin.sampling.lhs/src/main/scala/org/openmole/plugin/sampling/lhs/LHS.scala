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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.workflow.sampling.ScalableValue

object LHS {

  implicit def isSampling: IsSampling[LHS] = lhs => {
    def apply = FromContext { p =>
      import p._
      val s = lhs.sample.from(context)
      val vectorSize = lhs.factor.map(_.size(context)).sum
      def values = LHS.lhsValues(vectorSize, s, random())
      values.map(v => ScalableValue.toVariables(lhs.factor, v).from(context)).iterator
    }

    Sampling(
      apply,
      lhs.factor.map(_.prototype),
      lhs.factor.flatMap(_.inputs),
      lhs.sample.validate
    )
  }

  def lhsValues(dimensions: Int, samples: Int, rng: scala.util.Random) = Array.fill(dimensions) {
    org.openmole.tool.random.shuffled(0 until samples)(rng).map { i => (i + rng.nextDouble) / samples }.toArray
  }.transpose

}

case class LHS(sample: FromContext[Int], factor: Seq[ScalableValue])

