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
import org.openmole.tool.file._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.mole._
import java.io.File

import monocle.macros.Lenses
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.serializer._
import org.openmole.core.dsl
import dsl._

object LoadSource {

  implicit def isBuilder = new SourceBuilder[LoadSource] {
    override def name = LoadSource.name
    override def outputs = LoadSource.outputs
    override def inputs = LoadSource.inputs
    override def defaults = LoadSource.defaults
  }

  def apply(file: ExpandedString, prototypes: Prototype[_]*) =
    new LoadSource(
      file,
      prototypes.toVector,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    ) set (dsl.outputs += (prototypes: _*))

}

@Lenses case class LoadSource(
    file:       ExpandedString,
    prototypes: Vector[Prototype[_]],
    inputs:     PrototypeSet,
    outputs:    PrototypeSet,
    defaults:   DefaultSet,
    name:       Option[String]
) extends Source {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val from = new File(file.from(context))
    val loadedContext = SerialiserService.deserialiseAndExtractFiles[Context](from)
    context ++ prototypes.map(p ⇒ loadedContext.variable(p).getOrElse(throw new UserBadDataError(s"Variable $p has not been found in the loaded context")))
  }

}
