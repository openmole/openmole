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

package org.openmole.plugin.domain

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.tool.types._

import scala.reflect.ClassTag

package collection:

  // Avoid clash with iterableOfToArrayIsFix when T is of type Array[T]
  trait LowPriorityImplicits:
    implicit def iterableIsDiscrete[T]: DiscreteDomain[Iterable[T], T] = DiscreteDomain(domain => Domain(domain.iterator))
    implicit def iterableIsDiscreteFromContext[T]: DiscreteFromContextDomain[Iterable[T], T] = DiscreteFromContextDomain(domain => Domain(FromContext { _ ⇒ domain.iterator }))
    implicit def iterableIsFix[T]: FixDomain[Iterable[T], T] = FixDomain(domain => Domain(domain))
    implicit def iterableIsSized[T]: DomainSize[Iterable[T]] = DomainSize(domain => domain.size)

package object collection extends LowPriorityImplicits:

  implicit def rangeIsBounded: BoundedDomain[scala.Range, Int] = BoundedDomain(r => Domain((r.min, r.max)))
  implicit def doubleRangeIsBounded: BoundedDomain[DoubleRange, Double] = BoundedDomain(r => Domain((r.low, r.high)))

  implicit def rangeIsStep: DomainStep[scala.Range, Int] = DomainStep(r => r.step)
  implicit def doubleRangeIsStep: DomainStep[DoubleRange, Double] = DomainStep(r => r.step)

  implicit def iterableOfToArrayIsFinite[T: ClassTag, A1[_]: ToArray]: DiscreteFromContextDomain[Iterable[A1[T]], Array[T]] =
    DiscreteFromContextDomain: domain ⇒
      Domain(FromContext(_ => (domain.map(implicitly[ToArray[A1]].apply[T])).iterator))

  implicit def iterableOfToArrayIsFix[T: ClassTag, A1[_]: ToArray]: FixDomain[Iterable[A1[T]], Array[T]] = FixDomain(domain => Domain(domain.map(implicitly[ToArray[A1]].apply[T])))
  
  implicit def arrayIsDiscrete[T]: DiscreteFromContextDomain[Array[T], T] = DiscreteFromContextDomain(domain => Domain(FromContext { _ ⇒ domain.iterator }))
  implicit def arrayIsFix[T]: FixDomain[Array[T], T] = FixDomain(domain => Domain(domain.toIterable))
  implicit def arrayIsSized[T]: DomainSize[Array[T]] = DomainSize(domain => domain.size)
  //implicit def iteratorIsDiscrete[T]: DiscreteFromContextDomain[Iterator[T], T] = domain ⇒ Domain(domain)
  implicit def fromContextIteratorIsDiscrete[T]: DiscreteFromContextDomain[FromContext[Iterator[T]], T] = DiscreteFromContextDomain(domain => Domain(domain))

  implicit def booleanValIsFactor(p: Val[Boolean]): Factor[Vector[Boolean], Boolean] = Factor(p, Vector(true, false))

  implicit def arrayValIsFinite[T]: DiscreteFromContextDomain[Val[Array[T]], T] =
    DiscreteFromContextDomain: domain =>
      Domain(
        FromContext { p ⇒
          p.context(domain).iterator
        },
        inputs = Seq(domain)
      )

