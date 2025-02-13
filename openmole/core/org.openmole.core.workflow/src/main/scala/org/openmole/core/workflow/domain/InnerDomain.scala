package org.openmole.core.workflow.domain

/*
 * Copyright (C) 2025 Romain Reuillon
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

object InnerDomain:

  def apply[D, T](f: D => T) =
    new InnerDomain[D, T]:
      def apply(d: D) = f(d)

  given [D, W]: InnerDomain[Weight[D, W], D] = InnerDomain(_.value)
  given [D, W]: InnerDomain[By[D, W], D] = InnerDomain(_.value)

trait InnerDomain[I, D]:
  def apply(i: I): D
