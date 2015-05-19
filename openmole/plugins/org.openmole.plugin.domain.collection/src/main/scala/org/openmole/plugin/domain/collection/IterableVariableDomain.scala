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

package org.openmole.plugin.domain.collection

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._

import scala.util.Random

object IterableVariableDomain {

  def apply[T](variable: Prototype[Iterable[_ <: T]]) =
    new IterableVariableDomain[T](variable)

}

sealed class IterableVariableDomain[T](variable: Prototype[Iterable[_ <: T]]) extends Domain[T] with Discrete[T] {
  override def inputs = Seq(variable)
  override def iterator(context: Context)(implicit rng: RandomProvider): Iterator[T] = context(variable).iterator
}
