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

import monocle.Focus
import org.openmole.core.context.Variable
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.format.*
import org.openmole.core.argument.FromContext
import org.openmole.core.setter.*
import org.openmole.core.workflow.mole.{MoleExecutionContext, Source}

import scala.reflect.ClassTag

object CSVSource {

  implicit def isIO: InputOutputBuilder[CSVSource] = InputOutputBuilder(Focus[CSVSource](_.config))
  implicit def isInfo: InfoBuilder[CSVSource] = InfoBuilder(Focus[CSVSource](_.info))

  implicit def isCSV: MappedOutputBuilder[CSVSource] = new MappedOutputBuilder[CSVSource] {
    override def mappedOutputs = Focus[CSVSource](_.columns)
  }

  def apply(path: FromContext[String], separator: OptionalArgument[Char])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new CSVSource(
      path,
      config = InputOutputConfig(),
      info = InfoConfig(),
      columns = Vector.empty,
      separator = None
    )

}

case class CSVSource(
  path:      FromContext[String],
  config:    InputOutputConfig,
  info:      InfoConfig,
  columns:   Vector[ Mapped[?]],
  separator: Option[Char]
) extends Source {

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._

    val file = new File(path.from(context))
    val transposed =
      CSVFormat.csvToVariables(file, columns.map(_.toTuple.swap), separator).toSeq.transpose

    def variables =
      for {
        v ← transposed
      } yield {
        val p = v.head.prototype
        val content =
          if (v.isEmpty) Array()(ClassTag(p.`type`.runtimeClass))
          else v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass))

        Variable.unsecure(p.toArray, v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass)))
      }

    context ++ variables
  }

}

