package org.openmole.plugin.domain.distribution

/*
 * Copyright (C) 2025 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.domain.modifier.*
import scala.util.Random


object RandomSequence:

  given DiscreteFromContextDomain[RandomSequence[Int], Int] =
    DiscreteFromContextDomain[RandomSequence[Int], Int]: domain =>
      Domain(
        FromContext: p =>
          import p.*

          val r = p.random()

          Iterator.fill(domain.size):
            (domain.min.toOption, domain.max.toOption) match
              case (None, None) => r.nextInt()
              case (Some(m), None) => r.between(m, Int.MaxValue)
              case (None, Some(m)) => r.between(Int.MinValue, m)
              case (Some(min), Some(max)) => r.between(min, max)
      )

  given DiscreteFromContextDomain[RandomSequence[Long], Long] =
    DiscreteFromContextDomain[RandomSequence[Long], Long]: d =>
      Domain(
        FromContext: p =>
          import p.*

          val r = p.random()

          Iterator.fill(d.size):
            (d.min.toOption, d.max.toOption) match
              case (None, None) => r.nextLong()
              case (Some(m), None) => r.between(m, Long.MaxValue)
              case (None, Some(m)) => r.between(Long.MinValue, m)
              case (Some(min), Some(max)) => r.between(min, max)
      )

  given DiscreteFromContextDomain[RandomSequence[Double], Double] =
    DiscreteFromContextDomain[RandomSequence[Double], Double]: domain =>
      Domain(
        FromContext: p =>
          import p.*

          val r = p.random()

          Iterator.fill(domain.size):
            (domain.min.toOption, domain.max.toOption) match
              case (None, None) => r.nextDouble()
              case (Some(m), None) => r.between(m, 1.0)
              case (None, Some(m)) => r.between(0.0, m)
              case (Some(min), Some(max)) => r.between(min, max)
      )

  given [T]: DiscreteDomainModifiers[RandomSequence[T]] with {}



case class RandomSequence[T](size: Int, min: OptionalArgument[T] = None, max: OptionalArgument[T] = None)

