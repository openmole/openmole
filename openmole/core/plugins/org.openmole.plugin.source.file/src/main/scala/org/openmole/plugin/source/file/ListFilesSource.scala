/*
 * Copyright (C) 11/06/13 Romain Reuillon
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

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import java.io.File
import org.openmole.core.model.mole._
import org.openmole.core.implementation.tools.{ExpandedString, VariableExpansion}
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.mutable.ListBuffer

object ListFilesSource {

  def apply() = new ListFilesSourceBuilder

}
abstract class ListFilesSource extends Source {

  def directory: Seq[(ExpandedString, ExpandedString, Prototype[File])]

  override def process(context: Context, executionContext: ExecutionContext) =
    directory.map {
      case (path, regexp, prototype) ⇒
        val expandedPath = executionContext.relativise(path.from(context))
        val expandedRegExp = regexp.from(context)
        Variable(
          prototype.toArray,
          expandedPath.listFiles.filter(_.getName.matches(expandedRegExp))
        )

    }

}
