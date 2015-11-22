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
import org.openmole.core.workflow.tools.FromContext

object ShuffleDomain {

  implicit def isFinite[T, D] = new Finite[T, ShuffleDomain[T, D]] {
    override def computeValues(domain: ShuffleDomain[T, D]) = FromContext((context, rng) ⇒ domain.computeValues(context)(rng))
    override def inputs(domain: ShuffleDomain[T, D]) = domain.inputs
  }

  def apply[T, D](domain: D)(implicit finite: Finite[T, D]) = new ShuffleDomain[T, D](domain)

}

sealed class ShuffleDomain[+T, D](domain: D)(implicit finite: Finite[T, D]) {
  def inputs = finite.inputs(domain)
  def computeValues(context: Context)(implicit rng: RandomProvider): Iterable[T] =
    finite.iterator(domain).from(context).toSeq.shuffled(rng())
}
