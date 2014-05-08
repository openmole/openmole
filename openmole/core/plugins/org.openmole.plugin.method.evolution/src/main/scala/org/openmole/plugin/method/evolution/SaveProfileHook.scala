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
import java.io.File
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.service.Scaling._
import org.openmole.misc.tools.script.GroovyProxyPool
import org.openmole.core.implementation.mole._
import algorithm.{ GA ⇒ OMGA }
import fr.iscpif.mgo._

object SaveProfileHook {

  def apply(profile: OMGA[_ <: GA.GenomeProfile])(
    individual: Prototype[Individual[profile.G, profile.P, profile.F]],
    path: String) =
    new HookBuilder {
      addInput(individual.toArray)
      val _profile = profile
      val (_individual, _path) = (individual, path)

      def toHook = new SaveProfileHook with Built {
        val profile = _profile
        val individual = _individual.asInstanceOf[Prototype[Individual[profile.G, profile.P, profile.F]]]
        val path = _path
      }
    }

}

abstract class SaveProfileHook extends Hook with GenomeScaling {

  val profile: OMGA[_ <: GA.GenomeProfile]
  val individual: Prototype[Individual[profile.G, profile.P, profile.F]]
  val path: String
  def scales = profile.inputs

  def process(context: Context, executionContext: ExecutionContext) = {
    val file = executionContext.relativise(VariableExpansion(context, path))
    file.createParentDir
    file.withWriter { w ⇒
      for {
        i ← context(individual.toArray)
      } {
        val scaledGenome = scaled(profile.values.get(i.genome), context)
        w.write("" + scaledGenome(profile.algorithm.x).value + "," + profile.algorithm.aggregate(i.fitness) + "\n")
      }
    }
    context
  }

}
