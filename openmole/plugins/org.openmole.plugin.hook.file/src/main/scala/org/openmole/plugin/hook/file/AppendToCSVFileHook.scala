/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.hook.file

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.dsl._

object AppendToCSVFileHook {

  implicit def isIO: InputOutputBuilder[AppendToCSVFileHook] = InputOutputBuilder(AppendToCSVFileHook.config)

  implicit def isBuilder: AppendToCSVFileHookBuilder[AppendToCSVFileHook] = new AppendToCSVFileHookBuilder[AppendToCSVFileHook] {
    override def csvHeader = AppendToCSVFileHook.header
    override def arraysOnSingleRow = AppendToCSVFileHook.arraysOnSingleRow
  }

  def apply(file: FromContext[File], prototypes: Val[_]*)(implicit name: sourcecode.Name) =
    new AppendToCSVFileHook(
      file,
      prototypes.toVector,
      header = None,
      arraysOnSingleRow = false,
      config = InputOutputConfig()
    ) set (inputs += (prototypes: _*))

}

@Lenses case class AppendToCSVFileHook(
  file:              FromContext[File],
  prototypes:        Vector[Val[_]],
  header:            Option[FromContext[String]],
  arraysOnSingleRow: Boolean,
  config:            InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs) ++ header.toSeq.flatMap(_.validate(inputs))
  }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    import org.openmole.plugin.tool.csv._
    writeVariablesToCSV(file.from(context), prototypes, context, arraysOnSingleRow, header.map(_.from(context)))
    context
  }

}
