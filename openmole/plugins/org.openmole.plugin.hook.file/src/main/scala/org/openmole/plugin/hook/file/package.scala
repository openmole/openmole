/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.hook

import java.io.File

import org.openmole.core.context.Val
import org.openmole.core.dsl._
import org.openmole.core.setter._
import org.openmole.plugin.hook.file.CopyFileHook.{ CopyFileHookBuilder, CopyOptions }
import org.openmole.tool.outputredirection.OutputRedirection

package file {

  import org.openmole.core.argument.FromContext
  import org.openmole.core.workflow.hook.CSVHook

  trait FilePackage {

    def copies = new {
      def +=[T: CopyFileHookBuilder: InputOutputBuilder](prototype: Val[File], destination: FromContext[File], remove: Boolean = false, compress: Boolean = false, move: Boolean = false): T => T =
        (implicitly[CopyFileHookBuilder[T]].copies add ((prototype, destination, CopyOptions(remove, compress, move)))) andThen
          (inputs += prototype) andThen (if (move) (outputs += prototype) else identity)
    }

    lazy val AppendToCSVFileHook = CSVHook
  }
}

package object file extends FilePackage {
  private[file] def pack = this
}
