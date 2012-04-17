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

package org.openmole.plugin.hook.file

import java.io.File
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.tools.io.FileUtil._

class AppendArrayToFileHook(
  moleExecution: IMoleExecution,
  capsule: ICapsule,
  fileName: String,
  content: IPrototype[Array[_]]) extends CapsuleExecutionHook(moleExecution, capsule) {
  
  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    val file = new File(VariableExpansion.expandData(context,fileName))
    file.createParentDir
    val toWrite = context.value(content).getOrElse(Array("not found")).mkString(",")
    file.lockApply(_.appendLine(toWrite))
  }
  
  def inputs = DataSet(content)
  
}
