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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import java.io.File

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools._
import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }

object FileSource {

  implicit def isIO = InputOutputBuilder(FileSource.config)

  def apply(path: FromContext[String], prototype: Prototype[File]) =
    new FileSource(
      path,
      prototype,
      config = InputOutputConfig()
    ) set (dsl.outputs += prototype)

}

@Lenses case class FileSource(
    path:      FromContext[String],
    prototype: Prototype[File],
    config:    InputOutputConfig
) extends Source {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val expandedPath = new File(path.from(context))
    Variable(
      prototype,
      expandedPath
    )
  }
}
