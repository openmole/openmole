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
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.plugin.method.evolution.SavePopulationHook.resultVariables
import org.openmole.tool.file._
import org.openmole.tool.random.RandomProvider

object SavePopulationHook {

  def resultVariables(t: EvolutionWorkflow, context: Context)(implicit randomProvider: RandomProvider, newFile: NewFile, fileService: FileService) =
    context.variable(t.generationPrototype).toSeq ++ t.operations.result(context(t.populationPrototype).toVector, context(t.statePrototype)).from(context)

  def hook(t: EvolutionWorkflow, dir: FromContext[File], frequency: OptionalArgument[Long])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {
    FromContextHook("SavePopulationHook") { p ⇒
      import p._

      def save =
        frequency.option match {
          case None ⇒ true
          case Some(f) ⇒
            val generation = context(t.generationPrototype)
            (generation % f) == 0
        }

      if (save) {
        val resultFileLocation = dir / ExpandedString("population${" + t.generationPrototype.name + "}.csv")

        import org.openmole.plugin.tool.csv._

        val values = resultVariables(t, context).map(_.value)
        def headerLine = header(resultVariables(t, context).map(_.prototype.array), values)

        writeVariablesToCSV(
          resultFileLocation.from(context),
          headerLine,
          values,
          overwrite = true
        )
      }

      context
    } set (inputs += (t.populationPrototype, t.statePrototype))

  }

  def apply[T](algorithm: T, dir: FromContext[File], frequency: OptionalArgument[Long] = None)(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)
    hook(t, dir, frequency)
  }

}

object SaveLastPopulationHook {

  def apply[T](algorithm: T, file: FromContext[File])(implicit wfi: WorkflowIntegration[T], name: sourcecode.Name, definitionScope: DefinitionScope) = {
    val t = wfi(algorithm)

    FromContextHook("SaveLastPopulationHook") { p ⇒
      import p._
      import org.openmole.plugin.tool.csv._

      val values = resultVariables(t, context).map(_.value)
      def headerLine = header(resultVariables(t, context).map(_.prototype.array), values)

      writeVariablesToCSV(
        file.from(context),
        headerLine,
        values,
        overwrite = true
      )

      context
    } set (inputs += (t.populationPrototype, t.statePrototype))

  }

}

