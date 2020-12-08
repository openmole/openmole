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

package org.openmole.plugin.domain.distribution

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.dsl._

object UniformDistribution {
  implicit def isDiscrete[T]: DiscreteFromContext[UniformDistribution[T], T] = new DiscreteFromContext[UniformDistribution[T], T] {
    override def iterator(domain: UniformDistribution[T]) = domain.iterator
  }

  def apply[T](
    seed: OptionalArgument[Long] = None,
    max:  OptionalArgument[T]    = None
  )(implicit distribution: Distribution[T]) = new UniformDistribution(seed, max)

}

sealed class UniformDistribution[T](seed: Option[Long], max: Option[T])(implicit distribution: Distribution[T]) {

  def iterator = FromContext { p ⇒
    val random: scala.util.Random = seed match {
      case Some(s) ⇒ Random(s)
      case None    ⇒ p.random()
    }
    Iterator.continually {
      max match {
        case Some(i) ⇒ distribution.next(random, i)
        case None    ⇒ distribution.next(random)
      }
    }
  }
}
