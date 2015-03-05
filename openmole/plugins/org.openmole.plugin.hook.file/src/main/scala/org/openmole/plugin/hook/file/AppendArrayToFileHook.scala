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

package org.openmole.plugin.hook.file

import org.openmole.core.tools.io.FileUtil
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.ExpandedString
import FileUtil._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.mole.ExecutionContext

object AppendArrayToFileHook {

  def apply(fileName: ExpandedString, content: Prototype[Array[_]]) =
    new HookBuilder {
      addInput(content)
      def toHook = new AppendArrayToFileIHook(fileName, content) with Built
    }

}

abstract class AppendArrayToFileIHook(
    fileName: ExpandedString,
    content: Prototype[Array[_]]) extends Hook {

  override def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(fileName.from(context))
    file.createParentDir
    val toWrite = context.option(content).getOrElse(Array("not found")).mkString(",")
    file.withLock(_.appendLine(toWrite))
    context
  }

}
