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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.argument.OptionalArgument

object UniformDistribution:
  inline given[T]: DiscreteFromContextDomain[UniformDistribution[T], T] = domain =>
    Domain(
      FromContext: p ⇒
        import p._
        import domain._

        val distRandom: scala.util.Random =
          seed.option match
            case Some(s) ⇒ Random(s.from(context))
            case None    ⇒ p.random()

        Iterator.continually:
          max.option match
            case Some(i) ⇒ domain.distribution.next(distRandom, i)
            case None    ⇒ domain.distribution.next(distRandom)
      ,
      domain.seed.option.toSeq.flatMap(_.inputs),
      domain.seed.option.map(_.validate).toSeq
    )

case class UniformDistribution[T](
  seed: OptionalArgument[FromContext[Long]] = None,
  max: OptionalArgument[T] = None)(using val distribution: Distribution[T])
