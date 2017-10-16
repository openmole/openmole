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

import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.tools.math._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools._
import cats.implicits._

object LHS {

  def apply(samples: FromContext[Int], factors: ScalarOrSequence[_]*) =
    new LHS(samples, factors: _*)

  def lhsValues(dimensions: Int, samples: Int, rng: scala.util.Random) = Array.fill(dimensions) {
    org.openmole.tool.random.shuffled(0 until samples)(rng).map { i ⇒ (i + rng.nextDouble) / samples }.toArray
  }.transpose

}

sealed class LHS(val samples: FromContext[Int], val factors: ScalarOrSequence[_]*) extends Sampling {

  override def inputs = factors.flatMap(_.inputs)
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { p ⇒
    import p._
    val s = samples.from(context)
    val vectorSize = factors.map(_.size(context)).sum
    def values = LHS.lhsValues(vectorSize, s, random())
    values.map(v ⇒ ScalarOrSequence.scaled(factors, v).from(context)).toIterator
  }

}
