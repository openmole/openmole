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
import org.openmole.core.implementation.data.Parameter
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet

object NSGA2SteadyElitismTask {
  
  def apply[T <: GAGenome](  
    name: String, 
    individual: IPrototype[Individual[T, GAFitness]],
    archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]], 
    archiveSize: Int,
    nbGenerationSteady: IPrototype[Int],
    generation: IPrototype[Int],
    rank: Rank = new ParetoRank,
    dominance: Dominant = new StrictDominant)(implicit plugins: IPluginSet) = 
      new TaskBuilder { builder =>
        
      def toTask = new NSGA2SteadyElitismTask(
        name,
        individual,
        archive,
        archiveSize,
        nbGenerationSteady,
        generation,
        rank,
        dominance
      ) {
          
        val inputs = builder.inputs + archive + individual + nbGenerationSteady
        val outputs = builder.outputs + archive + individual + nbGenerationSteady
        val parameters: IParameterSet = 
          builder.parameters + 
          new Parameter(archive, Array.empty[Individual[T, GAFitness] with Ranking with Distance]) +
          new Parameter(nbGenerationSteady, 0) +
          new Parameter(generation, 0)
      }
    }
  
}


sealed abstract class NSGA2SteadyElitismTask[T <: GAGenome](
  val name: String, 
  individual: IPrototype[Individual[T, GAFitness]],
  archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]], 
  archiveSize: Int,
  nbGenerationSteady: IPrototype[Int],
  generation: IPrototype[Int],
  rank: Rank,
  dominance: Dominant)(implicit val plugins: IPluginSet)extends Task {

  override def process(context: IContext) = {
    val currentArchive =  context.valueOrException(archive)
    val globalArchive = context.valueOrException(individual) :: currentArchive.toList
    val newArchive = (NSGAII.elitism(globalArchive.toIndexedSeq, archiveSize, rank)(dominance)).toArray
    
    val steady = 
      if(Ranking.sameFirstRanked(currentArchive, newArchive, dominance)) context.valueOrException(nbGenerationSteady) + 1 else 0

    Context(new Variable(archive, newArchive.toArray), new Variable(nbGenerationSteady, steady), new Variable(generation, context.valueOrException(generation) + 1))
  }
  
  
}
