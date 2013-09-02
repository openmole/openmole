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

import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.core.implementation.data._

package object collection {

  implicit def scalaIterableDomainDecorator[T](iterable: Iterable[T]) = new {
    def toDomain = new IterableDomain[T](iterable)
  }

  implicit def prototypeDomainDecorator[T](variable: Prototype[Array[T]]) = new {
    def toDomain = new VariableDomain[T](variable)
    def toFactor = Factor(variable.fromArray, new VariableDomain[T](variable))
  }

  implicit def booleanPrototypeDecorator(p: Prototype[Boolean]) = new {
    def toFactor = Factor(p, List(true, false) toDomain)
  }

}