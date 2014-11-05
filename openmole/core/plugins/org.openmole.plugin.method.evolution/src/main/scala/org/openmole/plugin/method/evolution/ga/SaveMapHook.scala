/*
 * Copyright (C) 23/11/12 Romain Reuillon
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

package org.openmole.plugin.method.evolution.ga

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.mole.ExecutionContext
import org.openmole.misc.tools.io.FileUtil._

object SaveMapHook {

  def apply(p: GAParameters[GenomeMap], dir: String): HookBuilder =
    apply(p, dir, "map${" + p.generation.name + "}.csv")

  def apply(p: GAParameters[GenomeMap], dir: String, name: String): HookBuilder =
    new HookBuilder {
      addInput(p.population)
      val _path = dir + "/" + name

      def toHook = new SaveMapHook with Built {
        val gaParameters = p
        val path = _path
      }
    }

}

abstract class SaveMapHook extends Hook {

  val gaParameters: GAParameters[_ <: GenomeMap]
  val path: String

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(gaParameters.population).toIndividuals
      } {
        val scaledGenome = gaParameters.evolution.toVariables(i.genome, context)
        w.write("" + scaledGenome(gaParameters.evolution.x).value + "," + scaledGenome(gaParameters.evolution.y).value + "," + gaParameters.evolution.aggregate(i.fitness) + "\n")
      }
    }
    context
  }
}
