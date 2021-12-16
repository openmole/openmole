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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._

object UniformDistribution {

  implicit def isDiscrete[T]: DiscreteFromContextDomain[UniformDistribution[T], T] = domain ⇒
    Domain(
      FromContext { p ⇒
        import p._
        import domain._

        val distRandom: scala.util.Random = seed match {
          case Some(s) ⇒ Random(s.from(context))
          case None    ⇒ p.random()
        }

        Iterator.continually {
          max match {
            case Some(i) ⇒ distribution.next(distRandom, i)
            case None    ⇒ distribution.next(distRandom)
          }
        }
      },
      domain.seed.toSeq.flatMap(_.inputs),
      domain.seed.map(_.validate).toSeq
    )

//  def apply[T](
//    seed: OptionalArgument[FromContext[Long]] = None,
//    max:  OptionalArgument[T]                 = None
//  )(implicit distribution: Distribution[T]) = new UniformDistribution(seed, max, distribution)

}

case class UniformDistribution[T] (seed: Option[FromContext[Long]] = None, max: Option[T] = None)(implicit val distribution: Distribution[T])
