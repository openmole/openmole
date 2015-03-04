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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.io.FileUtil
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import java.io.File
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.serializer._
import FileUtil._
import collection.mutable.ListBuffer

object LoadSource {

  def apply(file: ExpandedString, prototypes: Prototype[_]*) =
    new SourceBuilder {
      prototypes.foreach(p ⇒ addOutput(p))
      def toSource = new LoadSource(file, prototypes: _*) with Built
    }

}

abstract class LoadSource(file: ExpandedString, prototypes: Prototype[_]*) extends Source {

  override def process(context: Context, executionContext: ExecutionContext) = {
    val from = executionContext.relativise(file.from(context))
    val loadedContext = SerialiserService.deserialiseAndExtractFiles[Context](from)
    context ++ prototypes.map(p ⇒ loadedContext.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found in the loaded context")))
  }

}
