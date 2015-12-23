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
import scalaz._
import Scalaz._

case class Replication[A](
  seed: Seeder = Seeder.empty,
  max: Int = 100,
  reevaluate: Double = 0.2,
  aggregation: Option[A] = None)

object WorkflowIntegration {

  implicit def unionContainingIntegrationRight[T, U](implicit wi: WorkflowIntegration[U]) = new WorkflowIntegration[\&/[T, U]] {
    def apply(t: \&/[T, U]) = wi(t)
  }

  def deterministicGAIntegration[STATE](a: DeterministicGA[STATE]) =
    new EvolutionWorkflow {
      override def algorithm: Algorithm[G, P, S] = a.algo

      type G = GAGenome
      def genomeType: PrototypeType[G] = PrototypeType[G]
      def randomGenome: State[Random, G] = ga.randomGenome(a.genome.size)

      type P = Seq[Double]
      def phenotypeType: PrototypeType[P] = PrototypeType[P]

      type S = STATE
      def stateType = a.stateType

      def inputPrototypes = a.genome.inputs.map(_.prototype)
      def outputPrototypes = a.objectives
      def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

      def genomeToVariables(genome: G) =
        GAIntegration.scaled(a.genome, genomeValues.get(genome))

      def populationToVariables(population: Population[Individual[G, P]]) =
        GAIntegration.populationToVariables[P](a.genome, a.objectives, identity)(population)

      def variablesToPhenotype(context: Context): P = a.objectives.map(o ⇒ context(o))

      def prepareIndividualForIsland(i: Ind) = i
    }

  def stochasticGAIntegration[STATE](a: StochasticGA[STATE]) =
    new EvolutionWorkflow {
      type G = GAGenome
      def genomeType: PrototypeType[G] = PrototypeType[G]
      def randomGenome: State[Random, G] = ga.randomGenome(a.genome.size)

      type P = History[Seq[Double]]
      override def phenotypeType: PrototypeType[P] = PrototypeType[P]

      override type S = STATE
      override def stateType = a.stateType

      def replications = Prototype[Int]("replications", namespace)

      def inputPrototypes = a.genome.inputs.map(_.prototype) ++ a.replication.seed.prototype
      def outputPrototypes: Seq[Prototype[_]] = a.objectives
      def resultPrototypes = (a.genome.inputs.map(_.prototype) ++ outputPrototypes ++ Seq(replications)).distinct

      override def algorithm: Algorithm[G, P, S] = a.algo

      def variablesToPhenotype(context: Context): P = History(a.objectives.map(o ⇒ context(o)))

      def phenotypeToValues(p: P): Seq[Double] =
        StochasticGAIntegration.aggregateSeq(a.replication.aggregation, p.history)

      def populationToVariables(population: Population[Individual[G, P]]) =
        StochasticGAIntegration.populationToVariables(a.genome, a.objectives, replications, phenotypeToValues)(population)

      def genomeToVariables(genome: G) =
        StochasticGAIntegration.genomeToVariables(a.genome, genomeValues.get(genome), a.replication.seed)

      def prepareIndividualForIsland(i: Ind) = i.copy(phenotype = i.phenotype.copy(age = 0))
    }

  case class DeterministicGA[STATE: Manifest](algo: Algorithm[ga.GAGenome, Seq[Double], STATE], genome: Genome, objectives: Objectives) {
    def stateType: PrototypeType[STATE] = PrototypeType[STATE]
  }

  implicit def DeterministicGAIntegration[STATE] = new WorkflowIntegration[DeterministicGA[STATE]] {
    def apply(a: DeterministicGA[STATE]) = WorkflowIntegration.deterministicGAIntegration(a)
  }

  case class StochasticGA[STATE: Manifest](algo: Algorithm[ga.GAGenome, History[Seq[Double]], STATE], genome: Genome, objectives: Objectives, replication: Replication[Seq[FitnessAggregation]]) {
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

  def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]]
  def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]]
  def prepareIndividualForIsland(i: Ind): Ind

  def randomGenome: State[Random, G]

  // Variables
  def namespace = Namespace("evolution")
  def genomePrototype = Prototype[G]("genome", namespace)(genomeType)
  def individualPrototype = Prototype[Ind]("individual", namespace)(individualType)
  def populationPrototype = Prototype[Pop]("population", namespace)(populationType)
  def offspringPrototype = Prototype[Pop]("offspring", namespace)(populationType)
  def statePrototype = Prototype[AlgoState]("state", namespace)(algoType)
  def generationPrototype = Prototype[Long]("generation", namespace)
  def terminatedPrototype = Prototype[Boolean]("terminated", namespace)
}

object GAIntegration {

  def variablesToPhenotype[P](objectives: Objectives, context: Context, valuesToPhenotype: Seq[Double] ⇒ P): P = {
    val scaled: Seq[(Prototype[Double], Double)] = objectives.map(o ⇒ o -> context(o))
    val phenotype: Seq[Double] = scaled.map(_._2)
    valuesToPhenotype(phenotype)
  }

  def scaled(inputs: Genome, values: Seq[Double]) = FromContext { (context, rng) ⇒
    InputConverter.scaled(inputs.inputs.toList, values.toList)(context, rng)
  }

  def genomesOfPopulationToVariables(inputs: Genome, population: Population[Individual[GAGenome, _]]) =
    for {
      scaledValues ← population.traverse[FromContext, List[Variable[_]]](i ⇒ scaled(inputs, genomeValues.get(i.genome)))
    } yield {
      inputs.inputs.zipWithIndex.map {
        case (input, i) ⇒
          input match {
            case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
            case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
          }
      }.toList
    }

  def objectivesOfPopulationToVariables[P](objectives: Objectives, phenotypeToValues: P ⇒ Seq[Double], population: Population[Individual[GAGenome, P]]) =
    objectives.zipWithIndex.map {
      case (p, i) ⇒
        Variable(
          p.toArray,
          population.map(ind ⇒ phenotypeToValues(ind.phenotype)(i)).toArray
        )
    }

  def populationToVariables[P](
    genome: Genome,
    objectives: Objectives,
    phenotypeToValues: P ⇒ Seq[Double])(population: Population[Individual[GAGenome, P]]) =
    GAIntegration.genomesOfPopulationToVariables(genome, population).map {
      _ ++ GAIntegration.objectivesOfPopulationToVariables(objectives, phenotypeToValues, population)
    }
}

object Seeder {

  def empty = new Seeder {
    def apply(rng: Random) = None
    def prototype = None
  }

  implicit def prototypeToSeeder[T](p: Prototype[T])(implicit seed: Seed[T]) = new Seeder {
    def apply(rng: Random) = Some(Variable(p, seed(rng)))
    def prototype = Some(p)
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

}

trait Seeder {
  def apply(rng: Random): Option[Variable[_]]
  def prototype: Option[Prototype[_]]
}

object StochasticGAIntegration {

  def aggregateSeq(aggregation: Option[Seq[FitnessAggregation]], values: Seq[Seq[Double]]): Seq[Double] =
    aggregation match {
      case Some(aggs) ⇒ (values zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ values.transpose.map(_.median)
    }

  def aggregate(aggregation: Option[FitnessAggregation], values: Seq[Double]): Double = aggregation.map(_(values)).getOrElse(values.median)

  def genomeToVariables(g: Genome, v: Seq[Double], seed: Seeder) =
    for {
      variables ← GAIntegration.scaled(g, v)
      s ← FromContext { (_, rng) ⇒ seed(rng()) }
    } yield variables ++ s

  def populationToVariables[PC](
    genome: Genome,
    objectives: Objectives,
    replications: Prototype[Int],
    phenotypeToValues: History[PC] ⇒ Seq[Double])(population: Population[Individual[GAGenome, History[PC]]]) =
    GAIntegration.populationToVariables[History[PC]](genome, objectives, phenotypeToValues)(population).map {
      _ ++ Seq(Variable(replications.toArray, population.map(_.phenotype.history.size).toArray))
    }

}

