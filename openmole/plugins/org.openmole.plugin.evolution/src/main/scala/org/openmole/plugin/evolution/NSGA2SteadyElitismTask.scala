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

package org.openmole.plugin.evolution

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.GAGenome
import fr.iscpif.mgo.ga.algorithm.NSGAII
import fr.iscpif.mgo.ga.domination.Dominant
import fr.iscpif.mgo.ga.domination.StrictDominant
import fr.iscpif.mgo.ga.selection.Distance
import fr.iscpif.mgo.ga.selection.Ranking
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

class NSGA2SteadyElitismTask[T <: GAGenome](
  name: String, 
  individual: IPrototype[Individual[T, GAFitness]],
  archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]],
  nbGenerationSteady: IPrototype[Int],
  archiveSize: Int,
  dominance: Dominant) extends Task(name) {

  def this(
    name: String, 
    individual: IPrototype[Individual[T, GAFitness]],
    archive: IPrototype[Array[Individual[T, GAFitness] with Ranking with Distance]],
    nbGenerationSteady: IPrototype[Int],
    archiveSize: Int) = this(name, individual, archive, nbGenerationSteady, archiveSize, new StrictDominant)
  
  
  addInput(archive)
  addInput(individual)
  addInput(nbGenerationSteady)
  addOutput(archive)
  addOutput(nbGenerationSteady)
  
  addParameter(archive, Array.empty[Individual[T, GAFitness] with Ranking with Distance])
  addParameter(nbGenerationSteady, 0)
  
  override def process(context: IContext) = {
    val newArchive = context.valueOrException(individual) :: context.valueOrException(archive).toList
    context + new Variable( 
      archive, (NSGAII.elitism(newArchive.toIndexedSeq, archiveSize)(dominance)).toArray
     )
  }
  
  
}
