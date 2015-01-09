/*
 * Copyright (C) 09/07/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.source.file

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import java.io.File
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.ExpandedString
import scala.collection.mutable.ListBuffer

object FileSource {

  def apply(path: ExpandedString, prototype: Prototype[File]) =
    new SourceBuilder {
      addOutput(prototype)
      def toSource = new FileSource(path, prototype) with Built
    }

}

abstract class FileSource(path: ExpandedString, prototype: Prototype[File]) extends Source {

  override def process(context: Context, executionContext: ExecutionContext) = {
    val expandedPath = executionContext.relativise(path.from(context))
    Variable(
      prototype,
      expandedPath
    )
  }
}
