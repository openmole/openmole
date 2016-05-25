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

import au.com.bytecode.opencsv.CSVReader
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import java.io._

import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.tool.csv.{ CSVToVariables, CSVToVariablesBuilder }

import collection.mutable.ListBuffer
import java.math._

import monocle.Lens
import monocle.macros.Lenses

import collection.JavaConversions._
import reflect.ClassTag
import org.openmole.core.workflow.mole.{ Source, SourceBuilder }
import org.openmole.core.workflow.mole.MoleExecutionContext
import org.openmole.core.dsl._

object CSVSource {

  implicit def isBuilder = new CSVToVariablesBuilder[CSVSource] with SourceBuilder[CSVSource] {
    override def columns = CSVSource.columns
    override def fileColumns = CSVSource.fileColumns
    override def separator = CSVSource.separator
    override def name = CSVSource.name
    override def outputs = CSVSource.outputs
    override def inputs = CSVSource.inputs
    override def defaults = CSVSource.defaults
  }

  def apply(path: ExpandedString) =
    new CSVSource(
      path,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None,
      columns = Vector.empty,
      fileColumns = Vector.empty,
      separator = None
    )

}

@Lenses case class CSVSource(
    path:        ExpandedString,
    inputs:      PrototypeSet,
    outputs:     PrototypeSet,
    defaults:    DefaultSet,
    name:        Option[String],
    columns:     Vector[(String, Prototype[_])],
    fileColumns: Vector[(String, File, Prototype[File])],
    separator:   Option[Char]
) extends Source with CSVToVariables {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider): Context = {
    val file = new File(path.from(context))
    val transposed = toVariables(file, context).toSeq.transpose

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

