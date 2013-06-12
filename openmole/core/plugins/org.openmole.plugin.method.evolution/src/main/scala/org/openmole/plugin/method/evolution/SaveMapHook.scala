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

object SaveMapHook {

  def apply(
    individual: Prototype[Individual[algorithm.GA#G, algorithm.GA#P, algorithm.GA#F]],
    map: GA.GAMap,
    scales: Seq[GenomeScaling.Scale],
    path: String) =
    new HookBuilder {
      addInput(individual.toArray)
      def toHook = new SaveMapHook(individual, map, scales, path) with Built
    }

}

//FIXME scala type system is not yet able to match the correct prototype (use a cast)
abstract class SaveMapHook(
    val individual: Prototype[Individual[algorithm.GA#G, algorithm.GA#P, algorithm.GA#F]],
    val map: GA.GAMap,
    val scales: Seq[GenomeScaling.Scale],
    val path: String) extends Hook with GenomeScaling {

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.directory.child(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(individual.toArray)
      } {
        val scaledGenome = scaled(i.genome.values, context)
        w.write("" + scaledGenome(map.x).value + "," + scaledGenome(map.y).value + "," + map.aggregation.aggregate(i.fitness) + "\n")
      }
    }
    context
  }
}
