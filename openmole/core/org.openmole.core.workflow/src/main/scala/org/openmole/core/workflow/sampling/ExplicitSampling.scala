/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.workflow.sampling

import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.argument.{ FromContext, Validate }

/**
 * An explicit sampling associates a prototype to an explicit set of values given through an iterable.
 * @param prototype Val to be sampled
 * @param data Iterable[T] explicit values of the sampling
 * @tparam T type of the Val
 */
object ExplicitSampling {

  implicit def isSampling[T]: IsSampling[ExplicitSampling[T]] = s =>
    Sampling(
      s.data.map { v => List(Variable(s.prototype, v)) }.iterator,
      Seq(s.prototype)
    )

}

case class ExplicitSampling[T](prototype: Val[T], data: Iterable[T])
