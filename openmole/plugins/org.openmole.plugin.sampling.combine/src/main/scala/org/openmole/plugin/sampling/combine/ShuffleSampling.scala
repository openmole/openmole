/*
 * Copyright (C) 2012 reuillon
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
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.sampling.ISampling
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

sealed class ShuffleSampling(sampling: ISampling) extends ISampling {
  
  override def inputs = sampling.inputs
  override def prototypes = sampling.prototypes
  
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    val random = context.valueOrException(openMOLERNG)
    shuffled(sampling.build(context).toList)(random).toIterator
  }
 
}