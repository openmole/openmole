/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow

/**
 * Sampling aims at associating prototypes with values.
 */
package sampling {

  import org.openmole.core.context._
  import org.openmole.core.expansion._
  import org.openmole.tool.types._
  import org.openmole.core.workflow.domain._
  import cats.implicits._

  trait SamplingPackage {

    implicit class PrototypeFactorDecorator[T](p: Val[T]) {
      def is(d: FromContext[T]) = Factor(p, d)
    }

    implicit def fromContextIsFinite[T] = new FiniteFromContext[FromContext[T], T] {
      override def computeValues(domain: FromContext[T]): FromContext[Iterable[T]] =
        domain.map(v ⇒ Vector(v))
    }

    implicit def discreteFactorIsSampling[D, T](f: Factor[D, T])(implicit discrete: DiscreteFromContext[D, T]) = FactorSampling(f)

    type Sampling = sampling.Sampling

    def EmptySampling() = sampling.EmptySampling()
  }
}

package object sampling {
  import org.openmole.core.context._
  import org.openmole.core.keyword._

  /**
   * The factor type associates a Val to a domain through the keyword In
   * @tparam D
   * @tparam T
   */
  type Factor[D, T] = In[Val[T], D]

  /**
   * Construct a [[Factor]] from a prototype and its domain
   * @param p
   * @param d
   * @return
   */
  def Factor[D, T](p: Val[T], d: D) = In(p, d)
}