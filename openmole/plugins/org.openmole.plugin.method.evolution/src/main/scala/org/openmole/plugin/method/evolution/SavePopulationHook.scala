/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.method.evolution

import org.openmole.core.context.Val
import org.openmole.core.expansion._
import org.openmole.core.workflow.dsl._
import org.openmole.core.context._
import monocle.macros._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.tool.file._

object SavePopulationHook {

  def apply[T](algorithm: T, dir: FromContext[File])(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name) = {
    val t = wfi(algorithm)

    FromContextHook("SavePopulationHook") { p ⇒
      import p._

      val resultFileLocation = dir / ExpandedString("population${" + t.generationPrototype.name + "}.csv")

      val resultVariables = t.operations.result(context(t.populationPrototype)).from(context)

      import org.openmole.plugin.tool.csv._

      writeVariablesToCSV(
        resultFileLocation.from(context),
        resultVariables.map(_.prototype.array),
        resultVariables
      )

      context
    } set (inputs += t.populationPrototype)

  }

}

