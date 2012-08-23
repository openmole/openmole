/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.plugin.task.template

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Data
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.workspace.Workspace
import org.openmole.core.implementation.tools.VariableExpansion._

abstract class AbstractTemplateFileTask extends Task {

  def output: IPrototype[File]

  override def process(context: IContext) = {
    val outputFile = Workspace.newFile("output", "template")
    expandBufferData(context, new FileInputStream(file(context)), new FileOutputStream(outputFile))
    Context.empty + (output, outputFile)
  }

  def file(context: IContext): File
}