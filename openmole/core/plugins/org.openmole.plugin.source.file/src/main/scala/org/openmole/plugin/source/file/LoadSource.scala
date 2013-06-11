/*
 * Copyright (C) 11/03/13 Romain Reuillon
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
import org.openmole.core.implementation.tools._
import org.openmole.core.model.mole._
import java.io.File
import org.openmole.core.implementation.tools._
import org.openmole.core.serializer._
import org.openmole.misc.tools.io.FileUtil._
import collection.mutable.ListBuffer

object LoadSource {

  def apply() =
    new SourceBuilder {
      private val _load = ListBuffer[(String, Prototype[_])]()

      def load(f: String, p: Prototype[_]) = {
        addOutput(p)
        _load += f -> p
      }

      def toSource =
        new LoadSource with Built {
          val load = _load.toList
        }
    }

}

abstract class LoadSource extends Source {

  def load: List[(String, Prototype[_])]

  override def process(context: Context, executionContext: ExecutionContext) = {
    load.map {
      case (f, p) ⇒
        val from = executionContext.directory.child(VariableExpansion(context, f))
        Variable.unsecure(p, SerializerService.deserializeAndExtractFiles(from))
    }

  }
}
