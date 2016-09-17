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

import org.openmole.tool.file._
import org.openmole.core.workflow.data.{ RandomProvider, Context }
import org.openmole.core.workflow.mole.{ HookBuilder, ExecutionContext, Hook }
import org.openmole.core.workflow.tools.FromContext[String]
import org.openmole.plugin.method.evolution._
import org.openmole.plugin.hook.file._

object SaveModelFamilyHook {

  def apply(parameters: GAParameters[ModelFamilyCalibration], dir: FromContext[String]) = {
    val fileName = dir + "/population${" + parameters.generation.name + "}.csv"
    new HookBuilder {
      def toHook =
        new SaveModelFamilyHook(parameters, fileName) with Built
    }
  }
}

abstract class SaveModelFamilyHook(parameters: GAParameters[ModelFamilyCalibration], path: FromContext[String]) extends Hook {

  def mf = parameters.evolution
  def traitsHeader = mf.modelFamily.traits.map(_.getName).mkString(",")
  def headers = s"${traitsHeader},${mf.inputsPrototypes.map(_.name).mkString(",")},${mf.objectives.map(_.name).mkString(",")}"

  def process(context: Context, executionContext: ExecutionContext)(implicit rng: RandomProvider) = {

    def idArray: Array[Int] = context(mf.modelFamily.modelIdPrototype.toArray)
    def inputsArray = parameters.evolution.inputsPrototypes.map(p ⇒ context(p.toArray).toSeq).transpose
    def outputsArray = parameters.evolution.objectives.map(p ⇒ context(p.toArray).toSeq).transpose

    lazy val combinations = mf.modelFamily.traitsCombinations.map(_.toSet)

    val file = executionContext.relativise(path.from(context))
    file.createParentDir
    file.withWriter() { w ⇒
      w.write(headers + "\n")
      for {
        ((id, inputs), outputs) ← idArray zip inputsArray zip outputsArray
      } {
        lazy val combination = combinations(id)
        def traits = mf.modelFamily.traits.map(combination.contains).mkString(",")
        w.write(s"""$traits,${inputs.mkString(",")},${outputs.mkString(",")}\n""")
      }
    }
    context
  }

}