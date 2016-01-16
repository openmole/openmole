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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.FromContext

package object collection {

  implicit def iterableIsDiscrete[T] = new Finite[Iterable[T], T] {
    override def computeValues(domain: Iterable[T]) = domain
  }

  implicit def arrayIsFinite[T] = new Finite[Array[T], T] {
    override def computeValues(domain: Array[T]) = domain.toIterable
  }

  implicit def booleanPrototypeIsFactor(p: Prototype[Boolean]) = Factor(p, List(true, false))

  implicit def arrayPrototypeIsFinite[T] = new Finite[Prototype[Array[T]], T] with DomainInputs[Prototype[Array[T]]] {
    override def inputs(domain: Prototype[Array[T]]): PrototypeSet = Seq(domain)
    override def computeValues(domain: Prototype[Array[T]]) = FromContext.apply { (context, rng) ⇒
      context(domain).toIterable
    }
  }

}