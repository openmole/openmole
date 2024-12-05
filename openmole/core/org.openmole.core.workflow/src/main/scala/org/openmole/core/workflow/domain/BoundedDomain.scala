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

import org.openmole.core.argument._
import scala.annotation.implicitNotFound

/**
 * Property of being bounded for a domain
 *
 * @tparam D domain type
 * @tparam T variable type
 */
@implicitNotFound("${D} is not a bounded variation domain of type ${T}")
trait BoundedDomain[-D, +T]:
  def apply(domain: D): Domain[(T, T)]

object BoundedFromContextDomain:

  given boundsIsContextBounds[D, T](using bounds: BoundedDomain[D, T]): BoundedFromContextDomain[D, T] = d =>
    val domain = bounds(d)
    val (min, max) = domain.domain
    domain.copy(domain = (FromContext.value(min), FromContext.value(max)))


@implicitNotFound("${D} is not a bounded variation domain of type T | FromContext[${T}]")
trait BoundedFromContextDomain[-D, +T]:
  def apply(domain: D): Domain[(FromContext[T], FromContext[T])]
