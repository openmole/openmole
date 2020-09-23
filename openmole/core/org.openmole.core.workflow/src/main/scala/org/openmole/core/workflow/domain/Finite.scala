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

package org.openmole.core.workflow.domain

import org.openmole.core.expansion._

import scala.annotation.implicitNotFound
import cats._
import cats.implicits._
import org.openmole.core.expansion

import scala.language.higherKinds

/**
 * Property of being finite for a domain
 *
 * @tparam D domain type
 * @tparam T variable type
 */
@implicitNotFound("${D} is not a finite variation domain of type ${T}")
trait Finite[-D, +T] extends Discrete[D, T] {
  def computeValues(domain: D): collection.Iterable[T]
  override def iterator(domain: D) = computeValues(domain).iterator
}

object FiniteFromContext {

  implicit def finiteIsContextFinite[D, T](implicit finite: Finite[D, T]): FiniteFromContext[D, T] =
    new FiniteFromContext[D, T] {
      override def computeValues(domain: D): FromContext[Iterable[T]] = FromContext.value(finite.computeValues(domain))
    }

}

@implicitNotFound("${D} is not a finite variation domain of type FromContext[${T}]")
trait FiniteFromContext[-D, +T] extends DiscreteFromContext[D, T] {
  def computeValues(domain: D): FromContext[Iterable[T]]
  override def iterator(domain: D) = computeValues(domain).map(_.iterator)
}

