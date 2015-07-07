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
package org.openmole.plugin.domain.modifier

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.tools.service.Random._

object ShuffleDomain {

  def apply[T](domain: Domain[T] with Discrete[T] with Finite[T]) =
    new ShuffleDomain[T](domain)

}

sealed class ShuffleDomain[+T](val domain: Domain[T] with Discrete[T] with Finite[T]) extends Domain[T] with Finite[T] {
  override def inputs = domain.inputs
  override def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[T] =
    domain.iterator(context).toSeq.shuffled(rng())
}
