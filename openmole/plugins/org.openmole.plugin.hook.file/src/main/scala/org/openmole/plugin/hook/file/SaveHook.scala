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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.mole._
import org.openmole.core.serializer._
import org.openmole.core.workflow.tools._
import java.io.File

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.dsl._

object SaveHook {

  implicit def isIO = InputOutputBuilder(SaveHook.config)

  def apply(file: ExpandedString, prototypes: Prototype[_]*) =
    new SaveHook(
      file,
      prototypes.toVector,
      config = InputOutputConfig()
    )
}

@Lenses case class SaveHook(
    file:       ExpandedString,
    prototypes: Vector[Prototype[_]],
    config:     InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = file.validate(inputs)

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val saveContext: Context = prototypes.map(p ⇒ context.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found")))
    val to = new File(file.from(context))
    to.createParentDir
    SerialiserService.serialiseAndArchiveFiles(saveContext, to)
    context
  }
}

