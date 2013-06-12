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
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.misc.tools.io.FileUtil._
import scala.collection.mutable.ListBuffer

object ListFilesSource {

  def apply() =
    new SourceBuilder {
      private val _list = new ListBuffer[(String, String, Prototype[File])]

      def list(path: String, prototype: Prototype[File], regExp: String = ".*") = {
        addOutput(prototype.toArray)
        _list += ((path, regExp, prototype))
      }

      def toSource =
        new ListFilesSource with Built {
          val list = _list.toList
        }
    }

}
abstract class ListFilesSource extends Source {

  def list: Seq[(String, String, Prototype[File])]

  override def process(context: Context, executionContext: ExecutionContext) =
    list.map {
      case (path, regexp, prototype) ⇒
        val expandedPath = executionContext.directory.child(VariableExpansion(context, path))
        val expandedRegExp = VariableExpansion(context, regexp)
        Variable(
          prototype.toArray,
          expandedPath.listFiles.filter(_.getName.matches(expandedRegExp))
        )

    }

}
