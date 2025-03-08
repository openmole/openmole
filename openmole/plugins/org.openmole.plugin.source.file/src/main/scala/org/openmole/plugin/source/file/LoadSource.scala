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

import java.io.File

import monocle.Focus
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.dsl._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.argument.FromContext
import org.openmole.core.serializer._
import org.openmole.core.setter._
import org.openmole.core.workflow.mole._

object LoadSource {

  implicit def isIO: InputOutputBuilder[LoadSource] = InputOutputBuilder(Focus[LoadSource](_.config))
  implicit def isInfo: InfoBuilder[LoadSource] = InfoBuilder(Focus[LoadSource](_.info))

  def apply(file: FromContext[String], prototypes: Val[?]*)(implicit serializerService: SerializerService, name: sourcecode.Name, definitionScope: DefinitionScope) =
    new LoadSource(
      file,
      prototypes.toVector,
      config = InputOutputConfig(),
      info = InfoConfig(),
      serializerService = serializerService
    ) set (outputs ++= prototypes)

}

case class LoadSource(
  file:              FromContext[String],
  prototypes:        Vector[Val[?]],
  config:            InputOutputConfig,
  info:              InfoConfig,
  serializerService: SerializerService
) extends Source {

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters =>
    import parameters._
    val from = new File(file.from(context))
    val loadedContext = serializerService.deserializeAndExtractFiles[Context](from, deleteFilesOnGC = true, gz = false)
    context ++ prototypes.map(p => loadedContext.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found in the loaded context")))
  }

}
