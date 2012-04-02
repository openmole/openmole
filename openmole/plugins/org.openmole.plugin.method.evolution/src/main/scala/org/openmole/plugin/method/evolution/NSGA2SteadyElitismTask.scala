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
import fr.iscpif.mgo.ga.algorithm.NSGAII
import fr.iscpif.mgo.ga.domination.Dominant
import fr.iscpif.mgo.ga.domination.StrictDominant
import fr.iscpif.mgo.ga.selection.Distance
import fr.iscpif.mgo.ga.selection.Rank
import fr.iscpif.mgo.ga.selection.ParetoRank
import fr.iscpif.mgo.ga.selection.Ranking
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

class NSGA2SteadyElitismTask[T <: GAGenome](
  name: String, 
  individual: IPrototype[Individual[T, GAFitness]],
  archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]],
  nbGenerationSteady: IPrototype[Int],
  generation: IPrototype[Int],
  archiveSize: Int,
  rank: Rank,
  dominance: Dominant) extends Task(name) {

  def this(
    name: String, 
    individual: IPrototype[Individual[T, GAFitness]],
    archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]],
    nbGenerationSteady: IPrototype[Int],
    generation: IPrototype[Int],
    archiveSize: Int,
    rank: Rank
  ) = this(name, individual, archive, nbGenerationSteady, generation, archiveSize, rank, new StrictDominant)
  
  def this(
    name: String, 
    individual: IPrototype[Individual[T, GAFitness]],
    archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]],
    nbGenerationSteady: IPrototype[Int],
    generation: IPrototype[Int],
    archiveSize: Int) = this(name, individual, archive, nbGenerationSteady, generation, archiveSize, new ParetoRank, new StrictDominant)
  
  addInput(archive)
  addInput(individual)
  addInput(nbGenerationSteady)
  addOutput(archive)
  addOutput(nbGenerationSteady)
  addOutput(generation)
  
  addParameter(archive, Array.empty[Individual[T, GAFitness] with Ranking with Distance])
  addParameter(nbGenerationSteady, 0)
  addParameter(generation, 0)
  
  override def process(context: IContext) = {
    val currentArchive =  context.valueOrException(archive)
    val globalArchive = context.valueOrException(individual) :: currentArchive.toList
    val newArchive = (NSGAII.elitism(globalArchive.toIndexedSeq, archiveSize, rank)(dominance)).toArray
    
    val steady = 
      if(Ranking.sameFirstRanked(currentArchive, newArchive, dominance)) context.valueOrException(nbGenerationSteady) + 1 else 0

    Context(new Variable(archive, newArchive.toArray), new Variable(nbGenerationSteady, steady), new Variable(generation, context.valueOrException(generation) + 1))
  }
  
  
}
