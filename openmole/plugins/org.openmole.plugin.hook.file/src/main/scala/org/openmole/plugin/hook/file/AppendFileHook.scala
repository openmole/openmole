/*
 *  Copyright (C) 2010 Romain Reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.file

import org.openmole.core.exception.UserBadDataError
import org.openmole.tool.file._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import java.io.File
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.mole.ExecutionContext

/**
 * Appends a variable content to an existing file.
 * The content of toBeDumpedPrototype file is appended to the outputFile safely(
 * concurent accesses are treated).
 * In the case of directories, all the files of the original directory are append to the
 * files of the target one.
 */
object AppendFileHook {

  def apply(prototype: Prototype[File], outputFile: ExpandedString) =
    new HookBuilder {
      addInput(prototype)

      def toHook = new AppendFileHook(prototype, outputFile) with Built
    }

}

abstract class AppendFileHook(prototype: Prototype[File], outputFile: ExpandedString) extends Hook {

  override def process(context: Context, executionContext: ExecutionContext)(implicit rng: RandomProvider) = {
    context.option(prototype) match {
      case Some(from) ⇒

        val to = executionContext.relativise(outputFile.from(context))
        if (!from.exists) throw new UserBadDataError("The file " + from + " does not exist.")

        if (!to.exists) {
          if (from.isDirectory) to.mkdirs
          else {
            to.createParentDir
            to.createNewFile
          }
        }

        if (from.isDirectory && to.isDirectory) {
          val toFiles = to.list
          from.list foreach (f ⇒ {
            if (!toFiles.contains(f)) new File(f).createNewFile
            new File(to, f).lockAndAppendFile(new File(from, f))
          })
        }
        else if (from.isFile && to.isFile) to.lockAndAppendFile(from)
        else throw new UserBadDataError("The merge can only be done from a file to another or from a directory to another. (" + from.toString + " and " + to.toString + " found)")
      case None ⇒ throw new UserBadDataError("Variable not found " + prototype)
    }
    context
  }

}

