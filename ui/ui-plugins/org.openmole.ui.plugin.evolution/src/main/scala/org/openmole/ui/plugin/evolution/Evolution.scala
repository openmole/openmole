/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.ui.plugin.evolution

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigma
import fr.iscpif.mgo.ga.selection.Distance
import fr.iscpif.mgo.ga.selection.Ranking
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.mole.MasterCapsule
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.transition.EndExplorationTransition
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import org.openmole.plugin.evolution.NSGA2SteadyElitismTask
import org.openmole.plugin.evolution.NSGA2SteadySigmaBreedTask
import org.openmole.plugin.evolution.ScalingParetoTask
import org.openmole.plugin.evolution.ScalingGenomeTask
import org.openmole.plugin.evolution.SigmaGenomeSampling
import org.openmole.plugin.evolution.ToIndividualTask
import org.openmole.ui.plugin.transitionfactory.IPuzzleFirstAndLast
import org.openmole.ui.plugin.transitionfactory.PuzzleFirstAndLast
import scala.collection.immutable.TreeMap

object Evolution {

  class Scaling {
    var scales = TreeMap.empty[String, (Double, Double)]
    def add(protoName: String, min: Double, max: Double): Unit = scales += (protoName) -> (min, max)
    def add(proto: IPrototype[Double], min: Double, max: Double): Unit =  add(proto.name, min, max)
    def add(protoName: String, max: Double): Unit = add(protoName, 0, max)
    def add(proto: IPrototype[Double], max: Double): Unit = add(proto.name, max)
  }
  
  class Objectives {
    var objectives = List.empty[IPrototype[Double]]
    def add(o: IPrototype[Double]) = objectives ::= o
  }
  
  trait NSGA2Sigma extends IPuzzleFirstAndLast {
    def elitismCapsule: ICapsule
    def breedingCapsule: ICapsule
    def outputCapsule: ICapsule
    def steadySincePrototype: IPrototype[Int]
    def generationPrototype: IPrototype[Int]
  }
  
  def scaling = new Scaling
  def objectives = new Objectives
  
  def nsga2SigmaSteady(
    model: ICapsule,
    scaling: Scaling,
    objectives: Objectives,
    populationSize: Int,
    archiveSize: Int,
    maxGenerationsSteady: Int,
    distributionIndex: Double
  ): NSGA2Sigma = { 
    val genomeWithSigmaPrototype = new Prototype("genome", classOf[GAGenomeWithSigma])
    val individualPrototype = new Prototype("individual", classOf[Individual[GAGenomeWithSigma, GAFitness]])
    val archivePrototype = new Prototype("archive", classOf[Array[Individual[GAGenomeWithSigma, GAFitness] with Distance with Ranking]])
    val steadySinceProto = new Prototype("steadySince", classOf[Int])
    val fitnessProto = new Prototype("fitness", classOf[GAFitness])
    val generationProto = new Prototype("generation", classOf[Int])
    
    val genomeSize = scaling.scales.size
    
    val sampling = new SigmaGenomeSampling(genomeWithSigmaPrototype, genomeSize, populationSize)
    val explorationCapsule = new Capsule(new ExplorationTask("genomeExploration", sampling))

    val scalingTask = new ScalingGenomeTask("scalingGenome", genomeWithSigmaPrototype)
    scaling.scales.foreach { 
      case(name, (min, max)) =>
        scalingTask.scale(new Prototype(name, classOf[Double]), min, max)
    }
    
    val scalingCaps = new Capsule(scalingTask)
    
    val toIndividualTask = new ToIndividualTask("toIndividualTask", genomeWithSigmaPrototype, individualPrototype)
    objectives.objectives.reverse.foreach {
      o => toIndividualTask.objective(o)
    }
    
    val toIndividualCapsule = new Capsule(toIndividualTask)
    
    val elitismTask = new NSGA2SteadyElitismTask(
      "elitismTask", 
      individualPrototype,
      archivePrototype, 
      steadySinceProto,
      generationProto,
      archiveSize
    )
    
    val elitismCaps = new MasterCapsule(elitismTask, archivePrototype, steadySinceProto, generationProto)
    
    val scalingParetoTask = new ScalingParetoTask("scalingPareto", archivePrototype)
    scaling.scales.foreach { 
      case(name, (min, max)) =>
        scalingParetoTask.scale(new Prototype(name, classOf[Double]), min, max)
    }
    
    objectives.objectives.reverse.foreach {
      o => scalingParetoTask.objective(o)
    }
    
    scalingParetoTask.addInput(steadySinceProto)
    scalingParetoTask.addInput(generationProto)
    
    scalingParetoTask.addOutput(steadySinceProto)
    scalingParetoTask.addOutput(generationProto)
   
    val scalingParetoCapsule = new Capsule(scalingParetoTask)
      
    
    val breedingTask = new NSGA2SteadySigmaBreedTask(
      "breedingTask", 
      archivePrototype,
      genomeWithSigmaPrototype,
      genomeSize,
      distributionIndex
    )
    
    val breedingCaps = new Capsule(breedingTask)
    
    val endTask = new EmptyTask("endTask")
    val endCapsule = new StrainerCapsule(endTask)
    
    new ExplorationTransition(explorationCapsule, scalingCaps)
    new Transition(scalingCaps, model)
    new Transition(model, toIndividualCapsule)
    new Transition(toIndividualCapsule, elitismCaps)
    new Transition(elitismCaps, scalingParetoCapsule)
    new Transition(scalingParetoCapsule, breedingCaps)
    new Transition(breedingCaps, new Slot(scalingCaps))
    new EndExplorationTransition("steadySince >= " + maxGenerationsSteady, scalingParetoCapsule, endCapsule)
    
    new DataChannel(scalingCaps, toIndividualCapsule, genomeWithSigmaPrototype)
    new DataChannel(elitismCaps, breedingCaps, archivePrototype)
    new NSGA2Sigma {
      def first = explorationCapsule
      def last = endCapsule
      def elitismCapsule = elitismCaps
      def breedingCapsule = breedingCaps
      def outputCapsule = scalingParetoCapsule
      def steadySincePrototype = steadySinceProto
      def generationPrototype = generationProto
    }
  }
  
      
}
