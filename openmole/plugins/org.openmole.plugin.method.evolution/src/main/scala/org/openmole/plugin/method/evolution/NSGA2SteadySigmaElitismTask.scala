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

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo._
import fr.iscpif.mgo.diversity._
import fr.iscpif.mgo.ranking._

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.model.task.IPluginSet

object NSGA2SteadySigmaElitismTask {

  def apply(
    name: String,
    individual: IPrototype[Individual[GAGenomeWithSigma, Fitness]],
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, Fitness] with Diversity with Rank]],
    nsga2: NSGA2Sigma,
    generation: IPrototype[Int],
    generationSteady: IPrototype[Int],
    terminated: IPrototype[Boolean])(implicit plugins: IPluginSet) =
    new TaskBuilder { builder ⇒

      addInput(archive)
      addInput(individual)
      addInput(generationSteady)
      addInput(generation)
      addOutput(archive)
      addOutput(generationSteady)
      addOutput(generation)

      addParameter(archive -> Array.empty[Individual[GAGenomeWithSigma, Fitness] with Rank with Diversity])
      addParameter(generationSteady -> 0)
      addParameter(generation -> 0)

      def toTask = new NSGA2SteadySigmaElitismTask(
        name,
        individual,
        archive,
        nsga2,
        generation,
        generationSteady,
        terminated) {
        val inputs = builder.inputs
        val outputs = builder.outputs
        val parameters = builder.parameters
      }
    }

}

sealed abstract class NSGA2SteadySigmaElitismTask(
    val name: String,
    individual: IPrototype[Individual[GAGenomeWithSigma, Fitness]],
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, Fitness] with Diversity with Rank]],
    nsga2: NSGA2Sigma,
    generation: IPrototype[Int],
    generationSteady: IPrototype[Int],
    terminated: IPrototype[Boolean])(implicit val plugins: IPluginSet) extends Task {

  override def process(context: IContext) = {
    val currentArchive = context.valueOrException(archive)
    val globalArchive = context.valueOrException(individual) :: currentArchive.toList
    val newArchive = (nsga2.elitism(nsga2.toI(globalArchive.toIndexedSeq))).toArray

    val (term, steady) = nsga2.terminated(currentArchive, newArchive, context.valueOrException(generationSteady))
    val terminatedVariable = new Variable(terminated, term)
    val generationSteadyVariable = new Variable(generationSteady, steady)
    Context(new Variable(archive, newArchive.toArray), terminatedVariable, generationSteadyVariable, new Variable(generation, context.valueOrException(generation) + 1))
  }

}
