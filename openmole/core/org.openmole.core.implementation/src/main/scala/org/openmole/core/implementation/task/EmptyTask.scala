/*
 * Copyright (C) 2011 reuillon
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

import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.ParameterSet
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.task.IPluginSet

object EmptyTask {
  
  def apply(name: String)(implicit plugins: IPluginSet = PluginSet.empty) = 
    new TaskBuilder { builder =>
      def toTask = 
        new EmptyTask(name) {
          val inputs = builder.inputs
          val outputs = builder.outputs
          val parameters = builder.parameters
        }
    }
 
}

sealed abstract class EmptyTask(val name: String)(implicit val plugins: IPluginSet) extends Task {
  override def process(context: IContext) = context
}
