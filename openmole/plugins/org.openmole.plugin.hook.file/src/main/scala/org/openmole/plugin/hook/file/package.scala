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
import org.openmole.core.workflow.data.Prototype
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.hook.file.CopyFileHook.CopyFileHookBuilder
import org.openmole.core.dsl._
import org.openmole.core.workflow.builder._
import org.openmole.plugin.hook.file.CopyFileHook.CopyOptions

package file {

  trait FilePackage {

    def copies = new {
      def +=[T: CopyFileHookBuilder: InputOutputBuilder](prototype: Prototype[File], destination: ExpandedString, remove: Boolean = false, compress: Boolean = false, move: Boolean = false): T ⇒ T =
        (implicitly[CopyFileHookBuilder[T]].copies add ((prototype, destination, CopyOptions(remove, compress, move)))) andThen
          (inputs += prototype) andThen (if (move) (outputs += prototype) else identity)
    }

    def csvHeader = new {
      def :=[T: AppendToCSVFileHookBuilder](h: OptionalArgument[ExpandedString]) =
        implicitly[AppendToCSVFileHookBuilder[T]].csvHeader.set(h)
    }

    def arraysOnSingleRow = new {
      def :=[T: AppendToCSVFileHookBuilder](b: Boolean) =
        implicitly[AppendToCSVFileHookBuilder[T]].arraysOnSingleRow.set(b)
    }
  }
}

package object file extends FilePackage {
  private[file] def pack = this
}
