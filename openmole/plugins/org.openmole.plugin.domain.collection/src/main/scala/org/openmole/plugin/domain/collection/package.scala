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
    given [T]: DiscreteDomain[Iterable[T], T] = DiscreteDomain(domain => Domain(domain.iterator))
    given [T]: DiscreteFromContextDomain[Iterable[T], T] = DiscreteFromContextDomain(domain => Domain(FromContext { _ => domain.iterator }))
    given [T]: FixDomain[Iterable[T], T] = FixDomain(domain => Domain(domain))
    given [T]: DomainSize[Iterable[T]] = DomainSize(domain => domain.size)

package object collection extends LowPriorityImplicits:

  given BoundedDomain[scala.Range, Int] = BoundedDomain(r => Domain((r.min, r.max)))
  given BoundedDomain[DoubleRange, Double] = BoundedDomain(r => Domain((r.low, r.high)))

  given DomainStep[scala.Range, Int] = DomainStep(r => r.step)
  given DomainStep[DoubleRange, Double] = DomainStep(r => r.step)

  given [T: ClassTag, A1[_]: ToArray]: DiscreteFromContextDomain[Iterable[A1[T]], Array[T]] =
    DiscreteFromContextDomain: domain =>
      Domain(FromContext(_ => (domain.map(implicitly[ToArray[A1]].apply[T])).iterator))

  given [T: ClassTag, A1[_]: ToArray]: FixDomain[Iterable[A1[T]], Array[T]] = FixDomain(domain => Domain(domain.map(implicitly[ToArray[A1]].apply[T])))
  
  given [T]: DiscreteFromContextDomain[Array[T], T] = DiscreteFromContextDomain(domain => Domain(FromContext { _ => domain.iterator }))
  given [T]: FixDomain[Array[T], T] = FixDomain(domain => Domain(domain.toIterable))
  given [T]: DomainSize[Array[T]] = DomainSize(domain => domain.size)
  //implicit def iteratorIsDiscrete[T]: DiscreteFromContextDomain[Iterator[T], T] = domain => Domain(domain)
  given [T]: DiscreteFromContextDomain[FromContext[Iterator[T]], T] = DiscreteFromContextDomain(domain => Domain(domain))

  given Conversion[Val[Boolean], Factor[Vector[Boolean], Boolean]] = p => Factor(p, Vector(true, false))

  given [T]: DiscreteFromContextDomain[Val[Array[T]], T] =
    DiscreteFromContextDomain: domain =>
      Domain(
        FromContext { p =>
          p.context(domain).iterator
        },
        inputs = Seq(domain)
      )
