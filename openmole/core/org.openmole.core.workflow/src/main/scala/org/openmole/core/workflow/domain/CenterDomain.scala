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
 * Property of being centered for a domain
 * @tparam D
 * @tparam T
 */
@implicitNotFound("${D} is not a variation domain with a center of type ${T}")
trait CenterDomain[-D, +T] {
  def center(domain: D): T
}

object CenterFromContextDomain {

  implicit def centerIsContextCenter[D, T](implicit c: CenterDomain[D, T]): CenterFromContextDomain[D, T] =
    new CenterFromContextDomain[D, T] {
      def center(domain: D) = FromContext.value(c.center(domain))
    }

}

@implicitNotFound("${D} is not a variation domain with a center of type T | FromContext[${T}]")
trait CenterFromContextDomain[-D, +T] {
  def center(domain: D): FromContext[T]
}
