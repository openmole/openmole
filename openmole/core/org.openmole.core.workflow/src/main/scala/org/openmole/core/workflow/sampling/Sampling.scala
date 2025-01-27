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

package org.openmole.core.workflow.sampling

import org.openmole.core.context._
import org.openmole.core.argument._

object Sampling:
  given [T: IsSampling as isSampling]: Conversion[T, Sampling] = t => isSampling(t)

case class Sampling(
  sampling: FromContext[Iterator[Iterable[Variable[?]]]],
  outputs:  Iterable[Val[?]],
  inputs:   PrototypeSet                                 = PrototypeSet.empty,
  validate: Validate                                     = Validate.success)

trait IsSampling[-S]:
  def apply(s: S): Sampling

