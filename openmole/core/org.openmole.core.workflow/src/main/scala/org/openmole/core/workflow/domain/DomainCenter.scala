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
 * Property of being centered for a domain
 * @tparam D
 * @tparam T
 */
@implicitNotFound("${D} is not a variation domain with a center of type ${T}")
trait DomainCenter[-D, +T]:
  def apply(domain: D): T

object DomainCenterFromContext:
  given centerIsContextCenter[D, T](using c: DomainCenter[D, T]): DomainCenterFromContext[D, T] = d => FromContext.value(c(d))

  def apply[D, T](f: D => FromContext[T]) =
    new DomainCenterFromContext[D, T]:
      def apply(d: D) = f(d)

@implicitNotFound("${D} is not a variation domain with a center of type T | FromContext[${T}]")
trait DomainCenterFromContext[-D, +T]:
  def apply(domain: D): FromContext[T]
