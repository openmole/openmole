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

package object collection {

  implicit class ScalaIterableDomainDecorator[T](iterable: Iterable[T]) {
    def toDomain = new IterableDomain[T](iterable)
  }

  implicit class PrototypeDomainDecorator[T](p: Prototype[Array[T]]) {
    def toDomain = new VariableDomain[T](p)
    def toFactor = Factor(p.fromArray, new VariableDomain[T](p))
  }

  implicit class BooleanPrototypeDecorator(p: Prototype[Boolean]) {
    def toFactor = Factor(p, List(true, false).toDomain)
  }

  //implicit def iterableToDomain[T](i: Iterable[T]) = i.toDomain
  //implicit def prototypeArrayToDomain[T](p: Prototype[Array[T]]) = p.toDomain

  implicit def prototypeCollectionConverter[T](p: Prototype[T]) = new {
    def in(i: Iterable[T]): Factor[T, IterableDomain[T]] = p in (i.toDomain)
    def in(p2: Prototype[Array[T]]): Factor[T, VariableDomain[T]] = p in (p2.toDomain)
  }

}