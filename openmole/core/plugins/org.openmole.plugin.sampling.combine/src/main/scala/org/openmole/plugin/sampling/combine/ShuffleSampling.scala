/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.sampling.combine

import java.util.Random
import org.openmole.core.model.data._
import org.openmole.core.model.sampling._
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

object ShuffleSampling {

  def apply(sampling: Sampling) =
    new ShuffleSampling(sampling)

}

sealed class ShuffleSampling(val sampling: Sampling) extends Sampling {

  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes

  override def build(context: Context): Iterator[Iterable[Variable[_]]] = {
    val random = newRNG(context(openMOLESeed))
    shuffled(sampling.build(context).toList)(random).toIterator
  }

}