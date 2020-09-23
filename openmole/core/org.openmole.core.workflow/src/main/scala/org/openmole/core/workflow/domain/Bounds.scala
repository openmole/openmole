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

/**
 * Property of being bounded for a domain
 *
 * @tparam D domain type
 * @tparam T variable type
 */
@implicitNotFound("${D} is not a bounded variation domain of type ${T}")
trait Bounds[-D, +T] {
  def min(domain: D): T
  def max(domain: D): T
}

object BondsFromContext {

  implicit def boundsIsContextBounds[D, T](implicit bounds: Bounds[D, T]): BondsFromContext[D, T] =
    new BondsFromContext[D, T] {
      def min(d: D) = FromContext.value(bounds.min(d))
      def max(d: D) = FromContext.value(bounds.max(d))
    }

}

@implicitNotFound("${D} is not a bounded variation domain of type T | FromContext[${T}]")
trait BondsFromContext[-D, +T] {
  def min(domain: D): FromContext[T]
  def max(domain: D): FromContext[T]
}