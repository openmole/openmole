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

import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.implementation.data._
import org.openmole.misc.tools.io.FileUtil._
import fr.iscpif.mgo._
import java.io.File
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.script.GroovyProxyPool
import org.openmole.core.implementation.mole._

object SaveProfileHook {

  def apply(profile: GA.GenomeProfile)(
    individual: Prototype[Individual[profile.G, profile.P, profile.F]],
    scales: Inputs,
    path: String) =
    new HookBuilder {
      addInput(individual.toArray)
      val (_individual, _profile, _scales, _path) = (individual, profile, scales, path)

      def toHook = new SaveProfileHook with Built {
        val profile = _profile
        val individual = _individual.asInstanceOf[Prototype[Individual[profile.G, profile.P, profile.F]]]
        val scales = _scales
        val path = _path
      }
    }

}

abstract class SaveProfileHook extends Hook with GenomeScaling {

  val profile: GA.GenomeProfile
  val individual: Prototype[Individual[profile.G, profile.P, profile.F]]
  val scales: Inputs
  val path: String

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(individual.toArray)
      } {
        val scaledGenome = scaled(profile.values.get(i.genome), context)
        w.write("" + scaledGenome(profile.x).value + "," + profile.aggregate(i.fitness) + "\n")
      }
    }
    context
  }

}
