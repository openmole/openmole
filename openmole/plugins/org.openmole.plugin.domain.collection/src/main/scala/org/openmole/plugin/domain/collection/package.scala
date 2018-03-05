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

import org.openmole.core.context.{ Val, PrototypeSet }
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._

package object collection {

  implicit def iterableIsDiscrete[T] = new Finite[Iterable[T], T] {
    override def computeValues(domain: Iterable[T]) = domain
  }

  implicit def iterableIsFix[T] = new Fix[Iterable[T], T] {
    override def apply(domain: Iterable[T]): Iterable[T] = domain
  }

  implicit def iterableIsSized[T] = new Sized[Iterable[T]] {
    override def apply(domain: Iterable[T]) = domain.size
  }

  implicit def arrayIsFinite[T] = new Finite[Array[T], T] {
    override def computeValues(domain: Array[T]) = domain.toIterable
  }

  implicit def arrayIsFix[T] = new Fix[Array[T], T] {
    override def apply(domain: Array[T]): Iterable[T] = domain.toIterable
  }

  implicit def arrayIsSized[T] = new Sized[Array[T]] {
    override def apply(domain: Array[T]) = domain.size
  }

  implicit def iteratorIsDiscrete[T] = new Discrete[Iterator[T], T] {
    override def iterator(domain: Iterator[T]) = domain
  }

  implicit def fromContextIteratorIsDiscrete[T] = new Discrete[FromContext[Iterator[T]], T] {
    override def iterator(domain: FromContext[Iterator[T]]) = domain
  }

  implicit def booleanPrototypeIsFactor(p: Val[Boolean]) = Factor(p, List(true, false))

  implicit def arrayPrototypeIsFinite[T] = new Finite[Val[Array[T]], T] with DomainInputs[Val[Array[T]]] {
    override def inputs(domain: Val[Array[T]]): PrototypeSet = Seq(domain)
    override def computeValues(domain: Val[Array[T]]) = FromContext { p ⇒
      p.context(domain).toIterable
    }
  }

}