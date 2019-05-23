/*
 * Copyright (C) 19/12/12 Romain Reuillon
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

import javax.swing.JPopupMenu.Separator
import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.dsl._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole.{ MoleExecutionContext, Source }
import org.openmole.plugin.tool.csv._

import scala.reflect.ClassTag

object CSVSource {

  implicit def isIO = InputOutputBuilder(CSVSource.config)
  implicit def isInfo = InfoBuilder(CSVSource.info)

  implicit def isCSV = new CSVToVariablesBuilder[CSVSource] {
    override def mappedOutputs = CSVSource.columns
    override def fileColumns = CSVSource.fileColumns
    override def separator = CSVSource.separator
  }

  def apply(path: FromContext[String], separator: OptionalArgument[Char])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new CSVSource(
      path,
      config = InputOutputConfig(),
      info = InfoConfig(),
      columns = Vector.empty,
      fileColumns = Vector.empty,
      separator = None
    )

}

@Lenses case class CSVSource(
  path:        FromContext[String],
  config:      InputOutputConfig,
  info:        InfoConfig,
  columns:     Vector[Mapped[_]],
  fileColumns: Vector[(String, File, Val[File])],
  separator:   Option[Char]
) extends Source {

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    import org.openmole.core.csv

    val file = new File(path.from(context))
    val transposed =
      csv.csvToVariables(columns.map(_.toTuple.swap), fileColumns, separator)(file, context).toSeq.transpose

    def variables =
      for {
        v ← transposed
      } yield {
        val p = v.head.prototype
        val content =
          if (v.isEmpty) Array.empty
          else v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass))

        Variable.unsecure(p.toArray, v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass)))
      }

    context ++ variables
  }

}

