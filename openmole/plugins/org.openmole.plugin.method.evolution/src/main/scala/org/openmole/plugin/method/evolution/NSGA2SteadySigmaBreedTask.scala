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
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.workspace.Workspace

class NSGA2SteadySigmaBreedTask(
  name: String,
  archivePrototype: IPrototype[Array[Individual[GAGenomeWithSigma, GAFitness] with Ranking with Distance]],
  genomePrototype: IPrototype[GAGenomeWithSigma],
  genomeSize: Int,
  distributionIndex: Double, 
  random: Random) extends Task(name) {

  addInput(archivePrototype)
  addOutput(genomePrototype)
  
  def this(
    name: String, 
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, GAFitness] with Ranking with Distance]],
    genomePrototype: IPrototype[GAGenomeWithSigma],
    genomeSize: Int,
    distributionIndex: Double) = this(name, archive, genomePrototype, genomeSize, distributionIndex, Workspace.newRNG)
  
  def this(
    name: String,
    archive: IPrototype[Array[Individual[GAGenomeWithSigma, GAFitness] with Ranking with Distance]],
    genome: IPrototype[GAGenomeWithSigma],
    genomeSize: Int
  ) = this(name, archive, genome, genomeSize, 2)

  @transient lazy val mutation = new CoEvolvingSigmaValuesMutation[GAGenomeWithSigma, GAGenomeWithSigmaFactory]
  @transient lazy val crossover = new SBXBoundedCrossover[GAGenomeWithSigma, GAGenomeWithSigmaFactory](distributionIndex)
  @transient lazy val factory = new GAGenomeWithSigmaFactory(genomeSize)
  @transient lazy val selection = new BinaryTournamentNSGA2[Individual[GAGenomeWithSigma, _] with Distance with Ranking]

  override def process(context: IContext) = {
    val archive = context.valueOrException(archivePrototype)
    val newGenome = NSGAII.breed(archive, factory, 1, selection, mutation, crossover)(random).head
    context + new Variable(genomePrototype, newGenome)
  }
  
}
