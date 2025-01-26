/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.workflow.domain

import org.openmole.core.context._
import org.openmole.core.argument._
import cats.implicits._

object UnrolledDomain:

  given isDiscrete[D, T: Manifest]: DiscreteDomain[UnrolledDomain[D, T], Array[T]] =
    DiscreteDomain: domain =>
      Domain(
        domain = Seq(domain.discrete(domain.d).domain.toArray).iterator,
        inputs = domain.discrete(domain.d).inputs,
        validation = domain.discrete(domain.d).validation
      )

  def apply[D[_], T: Manifest](domain: D[T])(implicit discrete: DiscreteDomain[D[T], T]) =
    new UnrolledDomain[D[T], T](domain)


class UnrolledDomain[D, T: Manifest](val d: D)(implicit val discrete: DiscreteDomain[D, T])
