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
import org.openmole.tool.random._

object LHS {

  def apply[D](samples: FromContext[Int], factors: Factor[D, Double]*)(implicit bounds: Bounds[D, Double], domainInputs: DomainInputs[D]) =
    new LHS[D](samples, factors: _*)

}

sealed class LHS[D](val samples: FromContext[Int], val factors: Factor[D, Double]*)(implicit bounds: Bounds[D, Double], domainInputs: DomainInputs[D]) extends Sampling {

  override def inputs = PrototypeSet(factors.flatMap(f ⇒ domainInputs.inputs(f.domain)))
  override def prototypes = factors.map { _.prototype }

  override def apply() = FromContext { (context, rng) ⇒
    implicit val random = rng
    val s = samples.from(context)
    factors.map {
      f ⇒
        (0 until s).shuffled(rng()).map {
          i ⇒
            Variable(
              f.prototype,
              ((i + rng().nextDouble) / s).scale(bounds.min(f.domain).from(context), bounds.max(f.domain).from(context))
            )
        }
    }.transpose.toIterator

  }
}
