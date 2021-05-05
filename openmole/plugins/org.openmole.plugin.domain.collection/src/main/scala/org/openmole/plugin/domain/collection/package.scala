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

import org.openmole.core.context.{ PrototypeSet, Val }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain.{ DomainInput, _ }
import org.openmole.core.workflow.sampling._
import org.openmole.tool.types._

import scala.reflect.ClassTag

package collection {

  // Avoid clash with iterableOfToArrayIsFix when T is of type Array[T]
  trait LowPriorityImplicits {
    implicit def iterableIsDiscrete[T] = new DiscreteFromContextDomain[Iterable[T], T] {
      override def iterator(domain: Iterable[T]) = domain.iterator
    }

    implicit def iterableIsFix[T] = new FixDomain[Iterable[T], T] {
      override def apply(domain: Iterable[T]): Iterable[T] = domain
    }

    implicit def iterableIsSized[T] = new SizedDomain[Iterable[T]] {
      override def apply(domain: Iterable[T]) = domain.size
    }
  }
}

package object collection extends LowPriorityImplicits {

  implicit def iterableOfToArrayIsFinite[T: ClassTag, A1[_]: ToArray] = new DiscreteFromContextDomain[Iterable[A1[T]], Array[T]] {
    override def iterator(domain: Iterable[A1[T]]) = domain.map(implicitly[ToArray[A1]].apply[T]).iterator
  }

  implicit def iterableOfToArrayIsFix[T: ClassTag, A1[_]: ToArray] = new FixDomain[Iterable[A1[T]], Array[T]] {
    override def apply(domain: Iterable[A1[T]]) = domain.map(implicitly[ToArray[A1]].apply[T])
  }

  implicit def arrayIsDiscrete[T] = new DiscreteFromContextDomain[Array[T], T] {
    override def iterator(domain: Array[T]) = domain.iterator
  }

  implicit def arrayIsFix[T] = new FixDomain[Array[T], T] {
    override def apply(domain: Array[T]): Iterable[T] = domain.toIterable
  }

  implicit def arrayIsSized[T] = new SizedDomain[Array[T]] {
    override def apply(domain: Array[T]) = domain.size
  }

  implicit def iteratorIsDiscrete[T] = new DiscreteFromContextDomain[Iterator[T], T] {
    override def iterator(domain: Iterator[T]) = domain
  }

  implicit def fromContextIteratorIsDiscrete[T] = new DiscreteFromContextDomain[FromContext[Iterator[T]], T] {
    override def iterator(domain: FromContext[Iterator[T]]) = domain
  }

  implicit def booleanValIsFactor(p: Val[Boolean]) = Factor(p, Vector(true, false))

  implicit def arrayValIsFinite[T] = new DiscreteFromContextDomain[Val[Array[T]], T] with DomainInput[Val[Array[T]]] {
    override def apply(domain: Val[Array[T]]): PrototypeSet = Seq(domain)
    override def iterator(domain: Val[Array[T]]) = FromContext { p ⇒
      p.context(domain).iterator
    }
  }

}