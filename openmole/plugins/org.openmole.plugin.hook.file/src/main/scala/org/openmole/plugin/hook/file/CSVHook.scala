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

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.workflow.tools.WritableOutput

object CSVHook {

  def apply(output: WritableOutput, values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    apply(output, values.toVector)

  def apply(
    output:     WritableOutput,
    values:     Seq[Val[_]]                           = Vector.empty,
    exclude:    Seq[Val[_]]                           = Vector.empty,
    header:     OptionalArgument[FromContext[String]] = None,
    arrayOnRow: Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = {
    val headerWritten = CacheKey[Boolean]()

    Hook("CSVHook") { parameters ⇒
      import parameters._
      import org.openmole.plugin.tool.csv

      val excludeSet = exclude.map(_.name).toSet
      val ps =
        { if (values.isEmpty) context.values.map { _.prototype }.toVector else values }.filter { v ⇒ !excludeSet.contains(v.name) }
      val vs = ps.map(context(_))

      def headerLine = header.map(_.from(context)) getOrElse csv.header(ps, vs, arrayOnRow)

      output match {
        case WritableOutput.FileValue(file) ⇒
          val f = file.from(context)
          f.createParentDir
          val h = if (f.isEmpty) Some(headerLine) else None
          f.withPrintStream(append = true) { ps ⇒ csv.writeVariablesToCSV(ps, h, vs, arrayOnRow) }
        case WritableOutput.PrintStreamValue(ps) ⇒
          val header = Some(headerLine)
          csv.writeVariablesToCSV(ps, header, vs, arrayOnRow)
      }

      context
    } validate { p ⇒
      import p._
      WritableOutput.file(output).toSeq.flatMap(_.validate(inputs)) ++
        header.option.toSeq.flatMap(_.validate(inputs))
    } set (inputs += (values: _*))
  }

}
