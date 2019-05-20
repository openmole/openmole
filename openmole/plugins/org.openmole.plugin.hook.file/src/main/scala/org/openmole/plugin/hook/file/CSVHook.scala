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

object CSVHook {

  def apply(file: FromContext[File], values: Val[_]*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    apply(file, values.toVector)

  def apply(
    file:       FromContext[File],
    values:     Seq[Val[_]]                           = Vector.empty,
    exclude:    Seq[Val[_]]                           = Vector.empty,
    header:     OptionalArgument[FromContext[String]] = None,
    arrayOnRow: Boolean                               = false)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    Hook("CSVHook") { parameters ⇒
      import parameters._
      import org.openmole.plugin.tool.csv

      val excludeSet = exclude.map(_.name).toSet
      val ps = (if (values.isEmpty) context.values.map { _.prototype }.toVector else values).filter { v ⇒ !excludeSet.contains(v.name) }
      val vs = ps.map(context(_))

      def headerLine = header.map(_.from(context)) getOrElse csv.header(ps, vs, arrayOnRow)
      csv.writeVariablesToCSV(file.from(context), headerLine, vs, arrayOnRow)
      context
    } validate { p ⇒
      import p._
      file.validate(inputs) ++ header.option.toSeq.flatMap(_.validate(inputs))
    } set (inputs += (values: _*))

}
