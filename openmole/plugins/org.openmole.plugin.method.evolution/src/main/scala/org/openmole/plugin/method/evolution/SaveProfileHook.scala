/*
 * Copyright (C) 08/01/13 Romain Reuillon
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

package org.openmole.plugin.method.evolution

import org.openmole.core.tools.io.FileUtil
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ExpandedString
import FileUtil._

object SaveProfileHook {

  def apply(p: GAParameters[GenomeProfile], dir: ExpandedString): HookBuilder = {
    val path = dir + "/profile${" + p.generation.name + "}.csv"
    new HookBuilder {
      addInput(p.population)
      def toHook = new SaveProfileHook(p, path) with Built
    }
  }

}

abstract class SaveProfileHook(gaParameters: GAParameters[GenomeProfile], path: ExpandedString) extends Hook {

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(path.from(context))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(gaParameters.population).toIndividuals
      } {
        val scaledGenome = gaParameters.evolution.toVariables(i.genome, context)
        w.write("" + scaledGenome(gaParameters.evolution.x).value + "," + gaParameters.evolution.aggregate(i.fitness) + "\n")
      }
    }
    context
  }

}
