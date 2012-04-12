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

package org.openmole.plugin.builder.evolution

import fr.iscpif.mgo.Individual
import fr.iscpif.mgo.ga.GAFitness
import fr.iscpif.mgo.ga.algorithm.GAGenomeWithSigma
import fr.iscpif.mgo.ga.domination.NonStrictDominant
import fr.iscpif.mgo.ga.selection.Distance
import fr.iscpif.mgo.ga.selection.Rank
import fr.iscpif.mgo.ga.selection.ParetoRank
import fr.iscpif.mgo.ga.selection.ParetoCrowdingRank
import fr.iscpif.mgo.ga.selection.Ranking
import fr.iscpif.mgo.tools.Scaling._
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.DataChannel
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.mole.MasterCapsule
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.mole.StrainerCapsule
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.transition.EndExplorationTransition
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.model.IPuzzleFirstAndLast
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMole
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.builder.Builder
import org.openmole.plugin.method.evolution.NSGA2SteadyElitismTask
import org.openmole.plugin.method.evolution.NSGA2SteadySigmaBreedTask
import org.openmole.plugin.method.evolution.ScalingParetoTask
import org.openmole.plugin.method.evolution.ScalingGenomeTask
import org.openmole.plugin.method.evolution.SigmaGenomeSampling
import org.openmole.plugin.method.evolution.ToIndividualTask
import scala.collection.immutable.TreeMap
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.transition._

object Evolution {

  class Inputs(val inputs: Array[String]) {
    var scales = TreeMap.empty[String, (Double, Double)]
    
    def scale(protoName: String, min: Double, max: Double): Unit = scales += (protoName) -> (min, max)
    def scale(proto: IPrototype[Double], min: Double, max: Double): Unit =  scale(proto.name, min, max)
    def scale(protoName: String, max: Double): Unit = scale(protoName, 0, max)
    def scale(proto: IPrototype[Double], max: Double): Unit = scale(proto.name, max)
  }
  
  class Objectives {
    var objectives = List.empty[(IPrototype[Double], Double)]
    def add(o: IPrototype[Double], v: Double) = objectives ::= o -> v
  }
  
  abstract class NSGA2Sigma extends IPuzzleFirstAndLast { nsga2 =>
    def elitismCapsule: ICapsule
    def breedingCapsule: ICapsule
    def outputCapsule: ICapsule
    def steadySincePrototype: IPrototype[Int]
    def generationPrototype: IPrototype[Int]
    def inputs: Inputs
    def initialGenomes: IPrototype[Array[Array[Double]]]
    
    def initialPopulation = new InitialPopulation {
      def inputs = nsga2.inputs
      def initialGenomes = nsga2.initialGenomes
    }
  }
  
  abstract class InitialPopulation {
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
    
  }
  
  def inputs(inputs: Array[String]) = new Inputs(inputs)
  def inputs(inputs: Array[IPrototype[_]]) = new Inputs(inputs.map{_.name})
    
  def objectives = new Objectives
  
  def nsga2SigmaSteady(
    name: String,
    model: IPuzzleFirstAndLast,
    scaling: Inputs,
    objectives: Objectives,
    populationSize: Int,
    archiveSize: Int,
    maxGenerationsSteady: Int,
    distributionIndex: Double,
    rank: Rank
  ): NSGA2Sigma = { 
    val genomeWithSigmaPrototype = new Prototype(name + "Genome", classOf[GAGenomeWithSigma])
    val individualPrototype = new Prototype(name + "Individual", classOf[Individual[GAGenomeWithSigma, GAFitness]])
    val archivePrototype = new Prototype(name + "Archive", classOf[Array[Individual[GAGenomeWithSigma, GAFitness] with Distance with Ranking]])
    val steadySinceProto = new Prototype(name + "SteadySince", classOf[Int])
    val fitnessProto = new Prototype(name + "Fitness", classOf[GAFitness])
    val generationProto = new Prototype(name + "Generation", classOf[Int])
    val initialGenomeProto = new Prototype(name + "InitialGenomes", classOf[Array[Array[Double]]])
    val genomeSize = scaling.scales.size
    
    val firstCapsule = new StrainerCapsule(new EmptyTask(name + "First"))
    
    val sampling = new SigmaGenomeSampling(genomeWithSigmaPrototype, initialGenomeProto, genomeSize, populationSize)
    val exploreSampling = new ExplorationTask(name + "GenomeExploration", sampling) 
    val explorationCapsule = new Capsule(exploreSampling)

    val scalingTask = new ScalingGenomeTask(name + "ScalingGenome", genomeWithSigmaPrototype)
    
    scaling.inputs.map{
      name => (name, scaling.scales.getOrElse(name, throw new UserBadDataError("Scale not found for input " + name)))}.foreach { 
        case (name, (min, max)) => scalingTask.scale(new Prototype(name, classOf[Double]), min, max)
      }
      
    
    val scalingCaps = new Capsule(scalingTask)
    
    val toIndividualTask = new ToIndividualTask(name + "ToIndividual", genomeWithSigmaPrototype, individualPrototype)
    objectives.objectives.reverse.foreach {
      case (o, v) => toIndividualTask.objective(o, v)
    }
    
    val toIndividualCapsule = new Capsule(toIndividualTask)
    
    val elitismTask = new NSGA2SteadyElitismTask(
      name + "ElitismTask", 
      individualPrototype,
      archivePrototype,
      archiveSize,
      steadySinceProto,
      generationProto,
      rank
    )
    
    val elitismCaps = new MasterCapsule(elitismTask, archivePrototype, steadySinceProto, generationProto)
    
    val scalingParetoTask = new ScalingParetoTask(name + "ScalingPareto", archivePrototype)
    scaling.scales.foreach { 
      case(name, (min, max)) =>
        scalingParetoTask.scale(new Prototype(name, classOf[Double]), min, max)
    }
    
    objectives.objectives.reverse.foreach {
      case(o, _) => scalingParetoTask.objective(o)
    }
    
    scalingParetoTask.addInput(steadySinceProto)
    scalingParetoTask.addInput(generationProto)
    
    scalingParetoTask.addOutput(steadySinceProto)
    scalingParetoTask.addOutput(generationProto)
   
    val scalingParetoCapsule = new Capsule(scalingParetoTask)
      
    
    val breedingTask = new NSGA2SteadySigmaBreedTask(
      name + "Breeding", 
      archivePrototype,
      genomeWithSigmaPrototype,
      genomeSize,
      distributionIndex
    )
    
    val breedingCaps = new StrainerCapsule(breedingTask)
    
    val endTask = new EmptyTask(name + "End")
    val endCapsule = new StrainerCapsule(endTask)
    
    new Transition(firstCapsule, explorationCapsule)
    new ExplorationTransition(explorationCapsule, scalingCaps)
    new Transition(scalingCaps, model.first)
    new Transition(model.last, toIndividualCapsule, filter = Set(genomeWithSigmaPrototype.name))
    new Transition(toIndividualCapsule, elitismCaps)
    new Transition(elitismCaps, scalingParetoCapsule)
    new Transition(scalingParetoCapsule, breedingCaps)
    new Transition(breedingCaps, new Slot(scalingCaps))
    new EndExplorationTransition(scalingParetoCapsule, endCapsule, steadySinceProto.name + " >= " + maxGenerationsSteady)
    
    new DataChannel(scalingCaps, toIndividualCapsule)
    new DataChannel(elitismCaps, breedingCaps)

    new DataChannel(firstCapsule, model.first)
    new DataChannel(explorationCapsule, endCapsule)
    
    new NSGA2Sigma {
      def first = firstCapsule
      def last = endCapsule
      def elitismCapsule = elitismCaps
      def breedingCapsule = breedingCaps
      def outputCapsule = scalingParetoCapsule
      def steadySincePrototype = steadySinceProto
      def generationPrototype = generationProto
      def inputs = scaling
      def initialGenomes = initialGenomeProto
    }
  }
  
  
  def nsga2SigmaSteady(
    name: String,
    model: IPuzzleFirstAndLast,
    scaling: Inputs,
    objectives: Objectives,
    populationSize: Int,
    archiveSize: Int,
    maxGenerationsSteady: Int,
    distributionIndex: Double
  ): NSGA2Sigma = 
   nsga2SigmaSteady(
      name,
      model,
      scaling,
      objectives,
      populationSize,
      archiveSize,
      maxGenerationsSteady,
      distributionIndex,
      new ParetoRank
    )
  
  def nsga2DiversitySigmaSteady(
    name: String,
    model: IPuzzleFirstAndLast,
    scaling: Inputs,
    objectives: Objectives,
    populationSize: Int,
    archiveSize: Int,
    maxGenerationsSteady: Int,
    distributionIndex: Double
  ): NSGA2Sigma = 
    nsga2SigmaSteady(
      name,
      model,
      scaling,
      objectives,
      populationSize,
      archiveSize,
      maxGenerationsSteady,
      distributionIndex,
      new ParetoCrowdingRank
    )
  
  
}
