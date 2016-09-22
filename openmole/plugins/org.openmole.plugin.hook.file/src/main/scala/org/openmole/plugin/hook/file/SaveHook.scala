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

  def apply(file: FromContext[File], prototypes: Prototype[_]*) =
    new SaveHook(
      file,
      prototypes.toVector,
      config = InputOutputConfig()
    )
}

@Lenses case class SaveHook(
    file:       FromContext[File],
    prototypes: Vector[Prototype[_]],
    config:     InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Prototype[_]]) = file.validate(inputs)

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val saveContext: Context = prototypes.map(p ⇒ context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found")))
    val to = file.from(context)
    to.createParentDir
    SerialiserService.serialiseAndArchiveFiles(saveContext, to)
    context
  }
}

