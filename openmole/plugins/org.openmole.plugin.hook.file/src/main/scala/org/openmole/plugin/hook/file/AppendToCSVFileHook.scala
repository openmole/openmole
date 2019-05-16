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
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.dsl._

object AppendToCSVFileHook {

  implicit def isIO: InputOutputBuilder[AppendToCSVFileHook] = InputOutputBuilder(AppendToCSVFileHook.config)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder: AppendToCSVFileHookBuilder[AppendToCSVFileHook] = new AppendToCSVFileHookBuilder[AppendToCSVFileHook] {
    override def csvHeader = AppendToCSVFileHook.header
    override def arraysOnSingleRow = AppendToCSVFileHook.arraysOnSingleRow
  }

  def apply(file: FromContext[File], values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): AppendToCSVFileHook =
    AppendToCSVFileHook(
      file,
      values.toVector)

  def apply(
    file:       FromContext[File],
    values:     Seq[Val[_]]                           = Vector.empty,
    exclude:    Seq[Val[_]]                           = Vector.empty,
    header:     OptionalArgument[FromContext[String]] = None,
    arrayOnRow: Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new AppendToCSVFileHook(
      file,
      values.toVector,
      exclude = exclude,
      header = header,
      arraysOnSingleRow = arrayOnRow,
      config = InputOutputConfig(),
      info = InfoConfig()
    ) set (inputs += (values: _*))

}

@Lenses case class AppendToCSVFileHook(
  file:              FromContext[File],
  prototypes:        Seq[Val[_]],
  exclude:           Seq[Val[_]],
  header:            Option[FromContext[String]],
  arraysOnSingleRow: Boolean,
  config:            InputOutputConfig,
  info:              InfoConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs) ++ header.toSeq.flatMap(_.validate(inputs))
  }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    import org.openmole.plugin.tool.csv

    val ps =
      if (prototypes.isEmpty) context.values.map { _.prototype }.toVector
      else prototypes

    val excludeSet = exclude.map(_.name).toSet
    val values = ps.filter { v ⇒ !excludeSet.contains(v.name) }.map(context(_))

    def headerLine = header.map(_.from(context)) getOrElse csv.header(ps, values, arraysOnSingleRow)
    csv.writeVariablesToCSV(file.from(context), headerLine, values, arraysOnSingleRow)
    context
  }

}
