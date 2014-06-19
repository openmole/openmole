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

package org.openmole.plugin.method.evolution

import org.openmole.core.model.mole.{ ExecutionContext, IHook }
import org.openmole.core.model.job.IMoleJob
import fr.iscpif.mgo._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.misc.tools.service.Scaling._
import org.openmole.core.implementation.tools._
import org.openmole.core.implementation.mole._
import org.openmole.plugin.method.evolution.ga._

object SaveMapHook {

  def apply(puzzle: GAPuzzle[GenomeMap], path: String) =
    new HookBuilder {
      addInput(puzzle.individual.toArray)
      val _puzzle = puzzle
      val _path = path

      def toHook = new SaveMapHook with Built {
        val puzzle = _puzzle
        val path = _path
      }
    }

}

abstract class SaveMapHook extends Hook with GenomeScaling {

  val puzzle: GAPuzzle[_ <: GenomeMap]
  def scales: Inputs = puzzle.evolution.inputs
  val path: String

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(puzzle.individual.toArray)
      } {
        val scaledGenome = scaled(puzzle.evolution.values.get(i.genome), context)
        w.write("" + scaledGenome(puzzle.evolution.x).value + "," + scaledGenome(puzzle.evolution.y).value + "," + puzzle.evolution.aggregate(i.fitness) + "\n")
      }
    }
    context
  }
}
