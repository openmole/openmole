/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.plugin.method.modelfamily

import org.openmole.core.workflow.data.Context
import org.openmole.core.workflow.mole.{ HookBuilder, ExecutionContext, Hook }
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.method.evolution._
import org.openmole.plugin.hook.file._
import org.openmole.misc.tools.io.FileUtil._

object SaveModelFamilyHook {

  def apply(puzzle: GAPuzzle[ModelFamilyCalibration], dir: ExpandedString) = {
    val fileName = dir + "/population${" + puzzle.generation.name + "}.csv"
    new HookBuilder {
      def toHook =
        new SaveModelFamilyHook(puzzle, fileName) with Built
    }
  }
}

abstract class SaveModelFamilyHook(puzzle: GAPuzzle[ModelFamilyCalibration], path: ExpandedString) extends Hook {

  def mf = puzzle.parameters.evolution
  def headers = s"${mf.modelFamily.modelIdPrototype.name},traits,number of traits,${mf.inputsPrototypes.map(_.name).mkString(",")},${mf.objectives.map(_.name).mkString(",")}"

  def process(context: Context, executionContext: ExecutionContext) = {

    def idArray: Array[Int] = context(mf.modelFamily.modelIdPrototype.toArray)
    def inputsArray = puzzle.parameters.evolution.inputsPrototypes.map(p ⇒ context(p.toArray)).transpose
    def outputsArray = puzzle.parameters.evolution.objectives.map(p ⇒ context(p.toArray)).transpose

    val file = executionContext.relativise(path.from(context))
    file.createParentDir
    file.withWriter { w ⇒
      w.write(headers + "\n")
      for {
        ((id, inputs), outputs) ← idArray zip inputsArray zip outputsArray
      } {

        def traits = s"$id,${mf.modelFamily.traitsString(id)},${mf.modelFamily.traitsCombinations(id).size}"
        w.write(s"""$traits,${inputs.mkString(",")},${outputs.mkString(",")}\n""")
      }
    }
    context
  }

}