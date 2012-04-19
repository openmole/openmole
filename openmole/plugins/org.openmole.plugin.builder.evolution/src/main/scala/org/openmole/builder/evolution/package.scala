/*
 * Copyright (C) 2012 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.builder

import fr.iscpif.mgo._
import fr.iscpif.mgo.ga._
import fr.iscpif.mgo.ranking._
import fr.iscpif.mgo.diversity._
import fr.iscpif.mgo.tools.Scaling._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.sampling._
import org.openmole.core.model.domain._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.method.evolution._
import scala.collection.immutable.TreeMap
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.transition._
import collection.mutable.ListBuffer

package object evolution {
  

  /*class NSGA2Sigma extends Puzzle { nsga2 =>
   def elitismCapsule: ICapsule
   def breedingCapsule: ICapsule
   def outputCapsule: ICapsule
   def steadyPrototype: IPrototype[Int]
   def generationPrototype: IPrototype[Int]
   def archivePrototype
   def inputs: Inputs
   def initialGenomes: IPrototype[Array[Array[Double]]]
    
   def initialPopulation = new InitialPopulation {
   def inputs = nsga2.inputs
   def initialGenomes = nsga2.initialGenomes
   }
   }*/
  
  /*abstract class InitialPopulation {
   def inputs: Inputs
   def initialGenomes: IPrototype[Array[Array[Double]]]
    
   var initialPopulation = List.empty[Array[Double]]

   def add(g: Array[Double]) = {
   if(g.size != inputs.inputs.size) throw new UserBadDataError("Genome " + g.mkString(",") + " doesn't match the expected size of inputs " + inputs.inputs.size)
   initialPopulation ::= g
   }
    
   def unscaled(g: Array[Double]) = (g zip inputs.inputs) map { 
   case(g, i) => 
   val (min, max) = inputs.scales(i) 
   g.unscale(min, max) 
   }
    
   def get = Context.empty + new Variable(initialGenomes, initialPopulation.map{ unscaled }.toArray)
    
   }*/
  
  
  def nsga2SigmaSteady(
    name: String,
    model: Puzzle,
    distributionIndex: Double,
    steadySince: Int,
    archiveSize: Int,
    populationSize: Int,
    inputs: Iterable[(IPrototype[Double], (Double, Double))],
    objectives: Iterable[(IPrototype[Double], Double)]
  )(implicit plugins: IPluginSet) = { 
    val genomeWithSigmaPrototype = new Prototype[GAGenomeWithSigma](name + "Genome")
    val individualPrototype = new Prototype[Individual[GAGenomeWithSigma, Fitness]](name + "Individual")
    val archivePrototype = new Prototype[Array[Individual[GAGenomeWithSigma, Fitness] with Diversity with Rank]](name + "Archive")
    val steadySinceProto = new Prototype[Int](name + "SteadySince")
    val fitnessProto = new Prototype[Fitness](name + "Fitness")
    val generationProto = new Prototype[Int](name + "Generation")
    val terminatedProto = new Prototype[Boolean](name + "Terminated")
    val initialGenomeProto = new Prototype[Array[Array[Double]]](name + "InitialGenomes")
    //val genomeSize = inputs.scales.size
    
    val nsga = new NSGA2Sigma(distributionIndex,  steadySince, archiveSize, inputs.size)
    
    val firstCapsule = new StrainerCapsule(EmptyTask(name + "First"))

    val sampling = new GenomeSampling(genomeWithSigmaPrototype, nsga, populationSize)
    val exploreSampling = ExplorationTask(name + "GenomeExploration", sampling) 
    val explorationCapsule = new Capsule(exploreSampling)

    val scalingTask = ScalingGenomeTask(name + "ScalingGenome", genomeWithSigmaPrototype, inputs.toSeq: _*)
    val scalingCaps = new Capsule(scalingTask)
    
    val toIndividualTask = ToIndividualTask(name + "ToIndividual", genomeWithSigmaPrototype, individualPrototype)
    objectives.foreach {
      case (o, v) => toIndividualTask addObjective (o, v)
    }
    
    val toIndividualCapsule = new Capsule(toIndividualTask)
    
    val elitismTask = NSGA2SteadySigmaElitismTask(
      name + "ElitismTask", 
      individualPrototype,
      archivePrototype,
      nsga,
      generationProto,
      steadySinceProto,
      terminatedProto
    )
    
    val elitismCaps = new MasterCapsule(elitismTask, archivePrototype, steadySinceProto, generationProto)
    
    val scalingParetoTask = ScalingArchiveTask(name + "ScalingArchive", archivePrototype, inputs.toSeq: _*)

    objectives.foreach {
      case(o, _) => scalingParetoTask addObjective o
    }
    
    scalingParetoTask addInput steadySinceProto
    scalingParetoTask addInput generationProto
    
    scalingParetoTask addOutput steadySinceProto
    scalingParetoTask addOutput generationProto
   
    val scalingParetoCapsule = new Capsule(scalingParetoTask)
      
    
    val breedingTask = NSGA2SteadySigmaBreedTask(
      name + "Breeding", 
      archivePrototype,
      genomeWithSigmaPrototype,
      nsga
    )
    
    val breedingCaps = new StrainerCapsule(breedingTask)
    
    val endTask = EmptyTask(name + "End")
    val endCapsule = new StrainerCapsule(endTask)
    
    new Transition(firstCapsule, explorationCapsule)
    new ExplorationTransition(explorationCapsule, scalingCaps)
    new Transition(scalingCaps, model.first)
    new Transition(model.last, toIndividualCapsule, filter = Set(genomeWithSigmaPrototype.name))
    new Transition(toIndividualCapsule, elitismCaps)
    new Transition(elitismCaps, scalingParetoCapsule)
    new Transition(scalingParetoCapsule, breedingCaps)
    new Transition(breedingCaps, new Slot(scalingCaps))
    new EndExplorationTransition(scalingParetoCapsule, endCapsule, terminatedProto.name)
    
    new DataChannel(scalingCaps, toIndividualCapsule)
    new DataChannel(elitismCaps, breedingCaps)

    new DataChannel(firstCapsule, model.first)
    new DataChannel(explorationCapsule, endCapsule)
    
    new Puzzle(firstCapsule, endCapsule, model.selection, model.grouping) {
      def outputCapsule = scalingParetoCapsule
      def steady = steadySinceProto
      def generation = generationProto
    }
    
  }
  
  
  
}
