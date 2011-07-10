/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.task

import org.openmole.core.implementation.data.Data
import org.openmole.core.model.data.IContext
import org.openmole.core.model.sampling.ISampling
import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.model.data.IVariable

object ExplorationTask {
  val Sample = new Data[Iterable[Iterable[IVariable[_]]]]("Sample#", classOf[Iterable[Iterable[IVariable[_]]]])
}


class ExplorationTask(name: String, val sampling: ISampling) extends GenericTask(name) with IExplorationTask {

  addOutput(ExplorationTask.Sample)

  //If input prototype as the same name as the output it is erased
  override protected def process(context: IContext) ={
    val sampled = sampling.build(context)
    context + (ExplorationTask.Sample.prototype, sampled)
  }
 
}
