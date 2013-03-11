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

package org.openmole.plugin.hook.file

import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import collection.mutable.ListBuffer
import org.openmole.core.model.mole._
import org.openmole.misc.workspace._
import org.openmole.core.serializer._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.io.FileUtil._
import java.io.File

object XMLHook {

  def apply =
    new HookBuilder {
      private val _serialize = ListBuffer[(Prototype[_], String)]()

      def serialize(p: Prototype[_], f: String) = {
        addInput(p)
        _serialize += p -> f
      }

      def toHook =
        new XMLHook with Built {
          val serialize = _serialize.toList
        }
    }

}

abstract class XMLHook extends Hook {

  def serialize: List[(Prototype[_], String)]

  override def process(context: Context, executionContext: ExecutionContext) = {
    serialize.map {
      case (p, f) ⇒
        val to = executionContext.directory.child(new File(VariableExpansion(context, f)))
        SerializerService.serialize(context(p), to)

    }
    context
  }
}

