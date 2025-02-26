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

import monocle.Focus

import java.io.File
import org.openmole.core.context.*
import org.openmole.core.exception.*
import org.openmole.core.argument.*
import org.openmole.core.serializer.*
import org.openmole.core.setter.*
import org.openmole.core.workflow.hook.{Hook, HookExecutionContext}
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.validation.*
import org.openmole.tool.random.*
import org.openmole.tool.file.*

object SaveHook {

  implicit def isIO: InputOutputBuilder[SaveHook] = InputOutputBuilder(Focus[SaveHook](_.config))
  implicit def isInfo: InfoBuilder[SaveHook] = InfoBuilder(Focus[SaveHook](_.info))

  def apply(file: FromContext[File], prototypes: Val[?]*)(implicit serializerService: SerializerService, name: sourcecode.Name, definitionScope: DefinitionScope) =
    new SaveHook(
      file,
      prototypes.toVector,
      config = InputOutputConfig(),
      serializerService = serializerService,
      info = InfoConfig()
    )
}

case class SaveHook(
  file:              FromContext[File],
  prototypes:        Vector[Val[?]],
  config:            InputOutputConfig,
  serializerService: SerializerService,
  info:              InfoConfig
) extends Hook with ValidateHook {

  override def validate = file.validate

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters =>
    import parameters._
    val saveContext: Context = prototypes.map(p => context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found")))
    val to = file.from(context)
    to.createParentDirectory
    serializerService.serializeAndArchiveFiles(saveContext, to)
    context
  }
}

