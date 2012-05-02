/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import fr.iscpif.mgo._
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo.ranking._
import fr.iscpif.mgo.diversity._

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random._
import org.openmole.core.implementation.task.Task._

object NSGA2SteadySigmaBreedTask {

  def apply(
    name: String,
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, Fitness] with Rank with Diversity]],
    genome: IPrototype[GAGenomeWithSigma],
    nsga2: NSGA2Sigma)(implicit plugins: IPluginSet) = new TaskBuilder { builder ⇒
    addInput(archive)
    addOutput(genome)

    def toTask =
      new NSGA2SteadySigmaBreedTask(name, archive, genome, nsga2) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
  }

}

sealed abstract class NSGA2SteadySigmaBreedTask(
    val name: String,
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, Fitness] with Rank with Diversity]],
    genome: IPrototype[GAGenomeWithSigma],
    nsga2: NSGA2Sigma)(implicit val plugins: IPluginSet) extends Task {

  override def process(context: IContext) = {
    val rng = newRNG(context.valueOrException(openMOLESeed))
    val a = context.valueOrException(archive)
    val newGenome = nsga2.breed(a, 1)(rng).head
    context + new Variable(genome, newGenome)
  }

}
