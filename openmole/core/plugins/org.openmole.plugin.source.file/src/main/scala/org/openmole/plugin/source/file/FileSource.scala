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

import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import java.io.File
import org.openmole.core.model.mole._
import org.openmole.core.implementation.tools._
import scala.collection.mutable.ListBuffer

object FileSource {

  def apply() =
    new SourceBuilder {
      private val _list = new ListBuffer[(String, Prototype[File])]

      def add(path: String, prototype: Prototype[File]) = {
        addOutput(prototype)
        _list += ((path, prototype))
      }

      def toSource =
        new FileSource with Built {
          val add = _list.toList
        }
    }

}

trait FileSource extends Source {

  def add: Seq[(String, Prototype[File])]

  override def process(context: Context, executionContext: ExecutionContext) =
    add.map {
      case (path, prototype) ⇒
        val expandedPath = executionContext.relativise(VariableExpansion(context, path))
        Variable(
          prototype,
          expandedPath
        )

    }
}
