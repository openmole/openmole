/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
import fr.iscpif.mgo.algorithm._
import fr.iscpif.mgo.clone.History
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.TextClosure
import org.openmole.tool.statistics._
import scalaz._
import scala.util.Random
import ga._
import org.openmole.core.workflow.tools._

case class Replication(
  max: Int = 100,
  reevaluate: Double = 0.2,
  aggregation: Option[FitnessAggregation] = None)

object WorkflowIntegration {

  implicit def unionContainingIntegrationRight[T, U](implicit wi: WorkflowIntegration[U]) = new WorkflowIntegration[\&/[T, U]] {
    def apply(t: \&/[T, U]) = wi(t)
  }

  def deterministicGAIntegration[STATE: Manifest](a: Algorithm[GAGenome, Seq[Double], STATE], genome: Genome, objective: Objectives) = {
    val _objective = objective
    val _genome = genome

    new DeterministicGAAlgorithmIntegration {
      override def stateType = PrototypeType[STATE]
      override def genome: Genome = _genome
      override def objectives: Objectives = _objective
      override def algorithm: Algorithm[G, P, S] = a
      override type S = STATE
    }
  }

  def stochasticGAIntegration[STATE: Manifest](a: Algorithm[GAGenome, History[Seq[Double]], STATE], genome: Genome, objective: Objectives, replication: Replication) = {
    val _objective = objective
    val _genome = genome
    val _replication = replication

    new StochasticGAAlgorithm {
      override def replication = _replication
      override def stateType = PrototypeType[STATE]
      override def genome: Genome = _genome
      override def objectives: Objectives = _objective
      override def algorithm: Algorithm[G, P, S] = a
      override type S = STATE
    }
  }

  /*def deterministicGAIntegration[S](ga: Algorithm[GAGenome, Seq[Double], S]) = new DeterministicGAAlgorithm[S]

  implicit def gaWorkflowIntegration[P, S]: WorkflowIntegration[GAGenome, P, S, GAAlgorithm[P, S]] = new WorkflowIntegration[GAGenome, P, S, GAAlgorithm[P, S]] {
    override def resultPrototypes(algorithm: GAAlgorithm[P, S]): Seq[Prototype[_]] = algorithm.resultPrototypes
    override def inputsPrototypes(algorithm: GAAlgorithm[P, S]): Seq[Prototype[_]] = algorithm.inputsPrototypes
    override def outputPrototypes(algorithm: GAAlgorithm[P, S]): Seq[Prototype[_]] = algorithm.outputPrototypes
    override def variablesToPhenotype(algorithm: GAAlgorithm[P, S])(context: Context): P = algorithm.toPhenotype(context)
    override def populationToVariables(algorithm: GAAlgorithm[P, S])(population: algorithm.Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = algorithm.populationToVariables(population, context)
    override def genomeToVariables(algorithm: GAAlgorithm[P, S])(genome: GAGenome, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = algorithm.genomeToVariables(genome, context)
    override def populationType(algorithm: GAAlgorithm[P, S]): PrototypeType[algorithm.Pop] = algorithm.populationType
    override def individualType(algorithm: GAAlgorithm[P, S]): PrototypeType[algorithm.Ind] = algorithm.individualType
    override def genomeType(algorithm: GAAlgorithm[P, S]): PrototypeType[GAGenome] = algorithm.genomeType
    override def randomGenome(algorithm: GAAlgorithm[P, S]): State[Random, GAGenome] = ga.randomGenome(algorithm.genome.size)
  }*/

}

trait WorkflowIntegration[T] {
  def apply(t: T): EvolutionWorkflow
}

trait EvolutionWorkflow {
  type G
  type P
  type S

  type Ind = Individual[G, P]
  type Pop = Population[Ind]
  type AlgoState = AlgorithmState[S]

  def genomeType: PrototypeType[G]
  def phenotypeType: PrototypeType[P]
  def stateType: PrototypeType[S]

  private implicit def genomeManifest = genomeType.manifest
  private implicit def phenotypeManifest = phenotypeType.manifest
  private implicit def stateManifest = stateType.manifest

  def populationType: PrototypeType[Pop] = PrototypeType[Pop]
  def individualType: PrototypeType[Ind] = PrototypeType[Ind]
  def algoType: PrototypeType[AlgoState] = PrototypeType[AlgoState]

  def algorithm: Algorithm[G, P, S]
  def variablesToPhenotype(context: Context): P

  def inputsPrototypes: Seq[Prototype[_]]
  def resultPrototypes: Seq[Prototype[_]]
  def outputPrototypes: Seq[Prototype[_]]

  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
  def populationToVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]

  def randomGenome: State[Random, G]

  def genomePrototype = Prototype[G]("genome")(genomeType)
  def individualPrototype = Prototype[Ind]("individual")(individualType)
  def populationPrototype = Prototype[Pop]("population")(populationType)
  def offspringPrototype = Prototype[Pop]("offspring")(populationType)
  def statePrototype = Prototype[AlgoState]("state")(algoType)
  def generationPrototype = Prototype[Long]("generation")
  def terminatedPrototype = Prototype[Boolean]("terminated")
}

trait GAAlgorithmIntegration extends EvolutionWorkflow { wfi ⇒

  type G = GAGenome

  def genome: Genome
  def objectives: Objectives

  def variablesToPhenotype(context: Context): P = {
    val scaled: Seq[(Prototype[Double], Double)] = objectives.map(o ⇒ o -> context(o))
    val phenotype: Seq[Double] = scaled.map(_._2)
    toPhenotype(phenotype)
  }

  def genomeType: PrototypeType[G] = PrototypeType[G]

  def inputsPrototypes = genome.inputs.map(_.prototype)
  def outputPrototypes: Seq[Prototype[_]] = objectives
  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = scaled(genomeValues.get(genome), context)
  def scaled(genome: Seq[Double], context: Context)(implicit rng: RandomProvider): List[Variable[_]] = InputConverter.scaled(wfi.genome.inputs.toList, genome.toList, context)

  def randomGenome: State[Random, G] = ga.randomGenome(genome.size)

  def toPhenotype(s: Seq[Double]): P

}

trait DeterministicGAAlgorithmIntegration extends GAAlgorithmIntegration {

  type P = Seq[Double]

  def phenotypeType: PrototypeType[P] = PrototypeType[P]
  def toPhenotype(s: Seq[Double]) = s

  override def resultPrototypes = (inputsPrototypes ++ outputPrototypes).distinct

  def populationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    genome.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            population.map(ind ⇒ ind.phenotype(i)).toArray)
      }
  }
}

object StochasticGAAlgorithm {
  def aggregate(aggregation: Option[FitnessAggregation])(individual: Individual[GAGenome, History[Seq[Double]]]): Seq[Double] =
    aggregation match {
      case Some(aggs) ⇒ (individual.phenotype.history zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ individual.phenotype.history.transpose.map(_.median)
    }
}

trait StochasticGAAlgorithm extends GAAlgorithmIntegration {

  type P = History[Seq[Double]]
  def phenotypeType: PrototypeType[P] = PrototypeType[P]

  def replications = Prototype[Int]("replications")
  def replication: Replication

  override def resultPrototypes = (inputsPrototypes ++ outputPrototypes ++ Seq(replications)).distinct
  def toPhenotype(s: Seq[Double]) = History(List(s))

  def populationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    genome.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            population.map(ind ⇒ StochasticGAAlgorithm.aggregate(replication.aggregation)(ind)(i)).toArray)
      } ++ Seq(Variable(replications.toArray, population.map(_.phenotype.history.size).toArray))
  }
}