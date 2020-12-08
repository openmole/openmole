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
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.tool.types._

import scala.reflect.ClassTag

package collection {

  // Avoid clash with iterableOfToArrayIsFix when T is of type Array[T]
  trait LowPriorityImplicits {
    implicit def iterableIsDiscrete[T] = new FiniteFromContext[Iterable[T], T] {
      override def computeValues(domain: Iterable[T]) = domain
    }

    implicit def iterableIsFix[T] = new Fix[Iterable[T], T] {
      override def apply(domain: Iterable[T]): Iterable[T] = domain
    }

    implicit def iterableIsSized[T] = new Sized[Iterable[T]] {
      override def apply(domain: Iterable[T]) = domain.size
    }
  }
}

package object collection extends LowPriorityImplicits {

  implicit def iterableOfToArrayIsFinite[T: ClassTag, A1[_]: ToArray] = new FiniteFromContext[Iterable[A1[T]], Array[T]] {
    override def computeValues(domain: Iterable[A1[T]]) = domain.map(implicitly[ToArray[A1]].apply[T])
  }

  implicit def iterableOfToArrayIsFix[T: ClassTag, A1[_]: ToArray] = new Fix[Iterable[A1[T]], Array[T]] {
    override def apply(domain: Iterable[A1[T]]) = domain.map(implicitly[ToArray[A1]].apply[T])
  }

  implicit def arrayIsFinite[T] = new FiniteFromContext[Array[T], T] {
    override def computeValues(domain: Array[T]) = domain.toIterable
  }

  implicit def arrayIsFix[T] = new Fix[Array[T], T] {
    override def apply(domain: Array[T]): Iterable[T] = domain.toIterable
  }

  implicit def arrayIsSized[T] = new Sized[Array[T]] {
    override def apply(domain: Array[T]) = domain.size
  }

  implicit def iteratorIsDiscrete[T] = new DiscreteFromContext[Iterator[T], T] {
    override def iterator(domain: Iterator[T]) = domain
  }

  implicit def fromContextIteratorIsDiscrete[T] = new DiscreteFromContext[FromContext[Iterator[T]], T] {
    override def iterator(domain: FromContext[Iterator[T]]) = domain
  }

  implicit def booleanValIsFactor(p: Val[Boolean]) = Factor(p, Vector(true, false))

  implicit def arrayValIsFinite[T] = new FiniteFromContext[Val[Array[T]], T] with DomainInputs[Val[Array[T]]] {
    override def inputs(domain: Val[Array[T]]): PrototypeSet = Seq(domain)
    override def computeValues(domain: Val[Array[T]]) = FromContext { p ⇒
      p.context(domain).toIterable
    }
  }

}