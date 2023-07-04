/*
 * Copyright (C) 2011 mathieu
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

package org.openmole.plugin.sampling.csv

import java.io.File
import monocle.{ Lens, Focus }
import org.openmole.core.context.{ PrototypeSet, Val, Variable }
import org.openmole.core.expansion.{ FromContext, Validate }
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig, Mapped, MappedOutputBuilder }
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools._

object CSVSampling {

  implicit def isIO: InputOutputBuilder[CSVSampling] = InputOutputBuilder(Focus[CSVSampling](_.config))

  implicit def isBuilder: MappedOutputBuilder[CSVSampling] = new MappedOutputBuilder[CSVSampling] {
    override def mappedOutputs: Lens[CSVSampling, Vector[Mapped[_]]] = Focus[CSVSampling](_.columns)
  }

  implicit def isSampling: IsSampling[CSVSampling] = s ⇒
    Sampling(
      s.apply(),
      s.outputs,
      s.inputs,
      s.validate
    )

  def apply(file: FromContext[File], separator: OptionalArgument[Char] = None): CSVSampling =
    new CSVSampling(
      file,
      config = InputOutputConfig(),
      columns = Vector.empty,
      separator = separator.option
    )

  def apply(file: File): CSVSampling = apply(file: FromContext[File])
  def apply(directory: File, name: FromContext[String]): CSVSampling = apply(FileFromContext(directory, name))

}

case class CSVSampling(
  file:      FromContext[File],
  config:    InputOutputConfig,
  columns:   Vector[Mapped[_]],
  separator: Option[Char]
) {

  def validate = file.validate

  def inputs = config.inputs
  def outputs = config.outputs
  
  def apply() = FromContext { p ⇒
    import p._
    import org.openmole.core.csv.*

    CSV.csvToVariables(
      file.from(context),
      columns.map(_.toTuple.swap),
      separator)
  }

}
