/*
 * Copyright (C) 16/01/14 Romain Reuillon
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

package org.openmole.plugin.builder

import org.openmole.plugin.method.abc._
import fr.irstea.scalabc.algorithm.Lenormand
import org.openmole.core.model.data._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.mole._
import org.openmole.core.model.task.PluginSet

package object abc {

  def abc(
    name: String,
    algorithm: Lenormand with ABC.ABC,
    model: Puzzle)(implicit plugins: PluginSet) = {
    val statePrototype = Prototype[Lenormand#STATE](name + "State")
    val terminatedPrototype = Prototype[Boolean](name + "Terminated")

    val sampling = LenormandSampling(algorithm, statePrototype)
    val first = StrainerCapsule(EmptyTask(name + "First"))
    val last = StrainerCapsule(EmptyTask(name + "Last"))
    val exploration = Capsule(ExplorationTask(name + "Exploration", sampling))
    val analyse = Capsule(LenormandAnalyseTask(name + "Analyse", algorithm, statePrototype, terminatedPrototype))

    (first -- exploration -< model >- analyse -- last) +
      (analyse -- exploration) +
      (first oo model.first) +
      (first -- (last, terminatedPrototype.name + " == true"))
  }

}
