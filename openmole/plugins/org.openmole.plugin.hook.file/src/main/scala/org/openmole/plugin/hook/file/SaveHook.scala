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

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.expansion._
import org.openmole.core.serializer._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.tool.random._
import org.openmole.tool.file._

object SaveHook {

  implicit def isIO = InputOutputBuilder(SaveHook.config)
  implicit def isInfo = InfoBuilder(info)

  def apply(file: FromContext[File], prototypes: Val[_]*)(implicit serializerService: SerializerService, name: sourcecode.Name, definitionScope: DefinitionScope) =
    new SaveHook(
      file,
      prototypes.toVector,
      config = InputOutputConfig(),
      serializerService = serializerService,
      info = InfoConfig()
    )
}

@Lenses case class SaveHook(
  file:              FromContext[File],
  prototypes:        Vector[Val[_]],
  config:            InputOutputConfig,
  serializerService: SerializerService,
  info:              InfoConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs)
  }

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters ⇒
    import parameters._
    val saveContext: Context = prototypes.map(p ⇒ context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found")))
    val to = file.from(context)
    to.createParentDir
    serializerService.serializeAndArchiveFiles(saveContext, to)
    context
  }
}

