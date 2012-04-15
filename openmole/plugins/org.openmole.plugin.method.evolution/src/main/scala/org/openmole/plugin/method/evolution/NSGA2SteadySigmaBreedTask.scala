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
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.GAGenome
import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigma
import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigmaFactory
import fr.iscpif.mgo.ga.algorithm.NSGAII
import fr.iscpif.mgo.ga.operators.crossover.SBXBoundedCrossover
import fr.iscpif.mgo.ga.operators.mutation.CoEvolvingSigmaValuesMutation
import fr.iscpif.mgo.ga.selection.BinaryTournamentNSGA2
import fr.iscpif.mgo.ga.selection.Distance
import fr.iscpif.mgo.ga.selection.Ranking
import java.util.Random
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.workspace.Workspace

object NSGA2SteadySigmaBreedTask {
  
  def apply(
    name: String,
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, GAFitness] with Ranking with Distance]],
    genome: IPrototype[GAGenomeWithSigma],
    genomeSize: Int,
    distributionIndex: Double = 2, 
    random: Random = Workspace.newRNG
  )(implicit plugins: IPluginSet) = new TaskBuilder { builder =>
    def toTask = 
      new NSGA2SteadySigmaBreedTask(
        name,
        archive,
        genome,
        genomeSize,
        distributionIndex,
        random
      ) {
        val inputs = builder.inputs + archive
        val outputs = builder.outputs + genome
        val parameters = builder.parameters
      }
  }
  
}

sealed abstract class NSGA2SteadySigmaBreedTask(
  val name: String,
  archive: IPrototype[Array[Individual[GAGenomeWithSigma, GAFitness] with Ranking with Distance]],
  genome: IPrototype[GAGenomeWithSigma],
  genomeSize: Int,
  distributionIndex: Double, 
  random: Random)(implicit val plugins: IPluginSet) extends Task {

  @transient lazy val mutation = new CoEvolvingSigmaValuesMutation[GAGenomeWithSigma, GAGenomeWithSigmaFactory]
  @transient lazy val crossover = new SBXBoundedCrossover[GAGenomeWithSigma, GAGenomeWithSigmaFactory](distributionIndex)
  @transient lazy val factory = new GAGenomeWithSigmaFactory(genomeSize)
  @transient lazy val selection = new BinaryTournamentNSGA2[Individual[GAGenomeWithSigma, _] with Distance with Ranking]

  override def process(context: IContext) = {
    val a = context.valueOrException(archive)
    val newGenome = NSGAII.breed(a, factory, 1, selection, mutation, crossover)(random).head
    context + new Variable(genome, newGenome)
  }
  
}
