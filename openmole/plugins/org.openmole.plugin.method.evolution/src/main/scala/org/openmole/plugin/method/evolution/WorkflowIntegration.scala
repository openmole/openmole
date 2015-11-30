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
import org.openmole.tool.statistics._
import scalaz._
import scala.util.Random
import ga._
import org.openmole.core.workflow.tools._
import org.openmole.tool.types._

case class Replication(
  seed: WorkflowIntegration.Seeder = WorkflowIntegration.Seeder.empty,
  max: Int = 100,
  reevaluate: Double = 0.2,
  aggregation: Option[FitnessAggregation] = None)

object WorkflowIntegration {

  object Seeder {
    def empty = new Seeder {
      def apply(rng: Random) = None
      def prototype = None
    }

    implicit def prototypeToSeeder[T](p: Prototype[T])(implicit seed: Seed[T]) = new Seeder {
      def apply(rng: Random) = Some(Variable(p, seed(rng)))
      def prototype = Some(p)
    }

  }

  trait Seeder {
    def apply(rng: Random): Option[Variable[_]]
    def prototype: Option[Prototype[_]]
  }

  object Seed {

    implicit val longIsSeed = new Seed[Long] {
      override def apply(rng: Random): Long = rng.nextLong()
    }

    implicit val intIsSeed = new Seed[Int] {
      override def apply(rng: Random): Int = rng.nextInt()
    }
  }

  trait Seed[T] {
    def apply(rng: Random): T
  }

  implicit def unionContainingIntegrationRight[T, U](implicit wi: WorkflowIntegration[U]) = new WorkflowIntegration[\&/[T, U]] {
    def apply(t: \&/[T, U]) = wi(t)
  }

  def deterministicGAIntegration[STATE](a: DeterministicGA[STATE]) =
    new DeterministicGAAlgorithmIntegration {
      override def stateType = a.stateType
      override def genome: Genome = a.genome
      override def objectives: Objectives = a.objectives
      override def algorithm: Algorithm[G, P, S] = a.algo
      override type S = STATE
    }

  def stochasticGAIntegration[STATE](a: StochasticGA[STATE]) =
    new StochasticGAAlgorithm {
      override def replication = a.replication
      override def stateType = a.stateType
      override def genome: Genome = a.genome
      override def objectives: Objectives = a.objectives
      override def algorithm: Algorithm[G, P, S] = a.algo
      override type S = STATE
    }

  case class DeterministicGA[STATE: Manifest](algo: Algorithm[ga.GAGenome, Seq[Double], STATE], genome: Genome, objectives: Objectives) {
    def stateType: PrototypeType[STATE] = PrototypeType[STATE]
  }

  implicit def DeterministicGAIntegration[STATE] = new WorkflowIntegration[DeterministicGA[STATE]] {
    def apply(a: DeterministicGA[STATE]) = WorkflowIntegration.deterministicGAIntegration(a)
  }

  case class StochasticGA[STATE: Manifest](algo: Algorithm[ga.GAGenome, History[Seq[Double]], STATE], genome: Genome, objectives: Objectives, replication: Replication) {
    def stateType: PrototypeType[STATE] = PrototypeType[STATE]
  }

  implicit def OMStochasticGAIntegration[STATE] = new WorkflowIntegration[StochasticGA[STATE]] {
    override def apply(a: StochasticGA[STATE]) = WorkflowIntegration.stochasticGAIntegration(a)
  }

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

  def inputPrototypes: Seq[Prototype[_]]
  def resultPrototypes: Seq[Prototype[_]]
  def outputPrototypes: Seq[Prototype[_]]

  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
  def populationToVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]

  def randomGenome: State[Random, G]

  def namespace = Namespace("evolution")
  def genomePrototype = Prototype[G]("genome", namespace)(genomeType)
  def individualPrototype = Prototype[Ind]("individual", namespace)(individualType)
  def populationPrototype = Prototype[Pop]("population", namespace)(populationType)
  def offspringPrototype = Prototype[Pop]("offspring", namespace)(populationType)
  def statePrototype = Prototype[AlgoState]("state", namespace)(algoType)
  def generationPrototype = Prototype[Long]("generation", namespace)
  def terminatedPrototype = Prototype[Boolean]("terminated", namespace)
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

  def inputPrototypes: Seq[Prototype[_]]
  def outputPrototypes: Seq[Prototype[_]] = objectives
  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
  def scaled(genome: Seq[Double], context: Context)(implicit rng: RandomProvider): List[Variable[_]] = InputConverter.scaled(wfi.genome.inputs.toList, genome.toList, context)

  def randomGenome: State[Random, G] = ga.randomGenome(genome.size)

  def toPhenotype(s: Seq[Double]): P

  def genomesOfPopulationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider) = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    genome.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList
  }

}

trait DeterministicGAAlgorithmIntegration extends GAAlgorithmIntegration {

  type P = Seq[Double]

  def phenotypeType: PrototypeType[P] = PrototypeType[P]
  def toPhenotype(s: Seq[Double]) = s

  override def inputPrototypes = genome.inputs.map(_.prototype)
  override def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider) = scaled(genomeValues.get(genome), context)

  def populationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    genomesOfPopulationToVariables(population, context) ++
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

  def replications = Prototype[Int]("replications", namespace)
  def replication: Replication

  override def inputPrototypes = genome.inputs.map(_.prototype) ++ replication.seed.prototype
  override def resultPrototypes = (genome.inputs.map(_.prototype) ++ outputPrototypes ++ Seq(replications)).distinct

  def toPhenotype(s: Seq[Double]) = History(List(s))

  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider) =
    scaled(genomeValues.get(genome), context) ++ replication.seed(rng())

  def objectivesOfPopulationToVariables(population: Population[Individual[G, P]]) =
    objectives.zipWithIndex.map {
      case (p, i) ⇒
        Variable(
          p.toArray,
          population.map(ind ⇒ StochasticGAAlgorithm.aggregate(replication.aggregation)(ind)(i)).toArray)
    }

  def populationToVariables(population: Population[Individual[G, P]], context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] =
    genomesOfPopulationToVariables(population, context) ++
      objectivesOfPopulationToVariables(population) ++
      Seq(Variable(replications.toArray, population.map(_.phenotype.history.size).toArray))
}