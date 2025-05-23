package org.openmole.core.workflow.domain

import scala.annotation.implicitNotFound

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.core.keyword.*

object DomainStep:
  def apply[D, T](f: D => T) =
    new DomainStep[D, T]:
      def apply(d: D) = f(d)

  given [D, T]: DomainStep[By[D, T], T] = DomainStep(_.by)
  given [D]: DomainStep[By[D, Int], Int] = DomainStep(_ => 1)
  given intToDouble[D]: DomainStep[By[D, Int], Double] = DomainStep(_ => 1.0)
  given [D]: DomainStep[By[D, Double], Double] = DomainStep(_ => 1.0)


@implicitNotFound("${D} is not a domain with a defined step of type ${T}")
trait DomainStep[-D, +T] :
  def apply(domain: D): T