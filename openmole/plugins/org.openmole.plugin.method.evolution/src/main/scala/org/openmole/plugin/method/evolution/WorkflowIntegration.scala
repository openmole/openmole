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

import org.openmole.core.context._
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.dsl._

import scala.language.higherKinds
import scala.util.Random
import cats._
import cats.implicits._
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble

object GASeeder {

  def empty = new GASeeder {
    def apply(rng: Random) = None
    def prototype = None
  }

  implicit def prototypeToSeeder[T](p: Val[T])(implicit seed: Seed[T]) = new GASeeder {
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

trait GASeeder {
  def apply(rng: Random): Option[Variable[_]]
  def prototype: Option[Val[_]]
}

case class Stochastic[A[_]: Functor](
  seed:         GASeeder                                = GASeeder.empty,
  replications: Int                                     = 100,
  reevaluate:   Double                                  = 0.2,
  aggregation:  OptionalArgument[A[FitnessAggregation]] = None
)

object WorkflowIntegration {

  implicit def hlistContainingIntegration[H <: shapeless.HList, U](implicit hwi: WorkflowIntegrationSelector[H, U]) = new WorkflowIntegration[H] {
    def apply(h: H) = hwi.wi(hwi(h))
  }

  def deterministicGAIntegration[AG](a: DeterministicGA[AG]) =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = Vector[Double]
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context))

      def inputPrototypes = a.genome.inputs.map(_.prototype)
      def objectives = a.objectives
      def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

      def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] =
        GAIntegration.scaled(a.genome, operations.values(genome))

      def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]] =
        GAIntegration.populationToVariables[I](
          a.genome,
          a.objectives,
          operations.genomeValues,
          operations.phenotype
        )(population)

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ o.fromContext(context)).toVector
    }

  def stochasticGAIntegration[AG](a: StochasticGA[AG]) =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = Vector[Double]
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def samples = Val[Long]("samples", namespace)

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context))

      def inputPrototypes = a.genome.inputs.map(_.prototype) ++ a.replication.seed.prototype
      def objectives = a.objectives
      def resultPrototypes = (a.genome.inputs.map(_.prototype) ++ outputPrototypes ++ Seq(samples)).distinct

      def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] =
        StochasticGAIntegration.genomeToVariables(a.genome, operations.values(genome), a.replication.seed)

      def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]] =
        StochasticGAIntegration.populationToVariables[I](
          a.genome,
          a.objectives,
          operations.genomeValues,
          operations.phenotype,
          samples,
          integration.samples
        )(population)

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ o.fromContext(context)).toVector
    }

  case class DeterministicGA[AG](
    ag:         AG,
    genome:     UniqueGenome,
    objectives: Objectives
  )(implicit val algorithm: MGOAPI.Integration[AG, Vector[Double], Vector[Double]])

  object DeterministicGA {
    implicit def deterministicGAIntegration[AG] = new WorkflowIntegration[DeterministicGA[AG]] {
      def apply(a: DeterministicGA[AG]) = WorkflowIntegration.deterministicGAIntegration(a)
    }
  }

  case class StochasticGA[AG](
    ag:          AG,
    genome:      UniqueGenome,
    objectives:  Objectives,
    replication: Stochastic[Seq]
  )(
    implicit
    val algorithm: MGOAPI.Integration[AG, Vector[Double], Vector[Double]] with MGOAPI.Stochastic
  )

  object StochasticGA {
    implicit def stochasticGAIntegration[AG] = new WorkflowIntegration[StochasticGA[AG]] {
      override def apply(a: StochasticGA[AG]) = WorkflowIntegration.stochasticGAIntegration(a)
    }
  }

}

trait WorkflowIntegration[T] {
  def apply(t: T): EvolutionWorkflow
}

trait EvolutionWorkflow {

  type MGOAG
  def mgoAG: MGOAG

  val integration: MGOAPI.Integration[MGOAG, V, P]
  import integration._

  def operations = integration.operations(mgoAG)

  type G = integration.G
  type I = integration.I
  type S = integration.S
  type V
  type P

  type Pop = Vector[I]

  def genomeType = ValType[G]
  def stateType = ValType[S]
  def individualType = ValType[I]

  def populationType: ValType[Pop] = ValType[Pop]

  def buildIndividual(genome: G, context: Context): I

  def inputPrototypes: Seq[Val[_]]
  def resultPrototypes: Seq[Val[_]]

  def objectives: Seq[Objective]
  def outputPrototypes: Seq[Val[Double]] = objectives.map(_.prototype.withType[Double])

  def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]]
  def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]]

  // Variables
  def namespace = Namespace("evolution")
  def genomePrototype = Val[G]("genome", namespace)(genomeType)
  def individualPrototype = Val[I]("individual", namespace)(individualType)
  def populationPrototype = Val[Pop]("population", namespace)(populationType)
  def offspringPrototype = Val[Pop]("offspring", namespace)(populationType)
  def statePrototype = Val[S]("state", namespace)(stateType)
  def generationPrototype = Val[Long]("generation", namespace)
  def terminatedPrototype = Val[Boolean]("terminated", namespace)
}

object GAIntegration {

  def scaled(inputs: UniqueGenome, values: Seq[Double]) = ScalarOrSequenceOfDouble.scaled(inputs.inputs.toList, values.toList)

  def genomesOfPopulationToVariables[I](inputs: UniqueGenome, population: Vector[I], genomeValues: I ⇒ Vector[Double]) =
    for {
      scaledValues ← population.traverse[FromContext, List[Variable[_]]](i ⇒ scaled(inputs, genomeValues(i)))
    } yield {
      inputs.zipWithIndex.map { case (input, i) ⇒ input.toVariable(scaledValues.map(_(i).value)) }.toList
    }

  def objectivesOfPopulationToVariables[I](objectives: Objectives, population: Vector[I], phenotypeValues: I ⇒ Vector[Double]) =
    objectives.zipWithIndex.map {
      case (p, i) ⇒
        Variable(
          p.prototype.withType[Array[Double]],
          population.map(ind ⇒ phenotypeValues(ind)(i)).toArray
        )
    }

  def populationToVariables[I](
    genome:          UniqueGenome,
    objectives:      Objectives,
    genomeValues:    I ⇒ Vector[Double],
    phenotypeValues: I ⇒ Vector[Double]
  )(population: Vector[I]) =
    GAIntegration.genomesOfPopulationToVariables(genome, population, genomeValues).map {
      _ ++ GAIntegration.objectivesOfPopulationToVariables(objectives, population, phenotypeValues)
    }
}

object StochasticGAIntegration {

  def aggregateVector(aggregation: Option[Seq[FitnessAggregation]], values: Vector[Vector[Double]]): Vector[Double] =
    aggregation match {
      case Some(aggs) ⇒ (values.transpose zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ values.transpose.map(_.median)
    }

  def aggregate(aggregation: Option[FitnessAggregation], values: Seq[Double]): Double = aggregation.map(_(values)).getOrElse(values.median)

  def genomeToVariables(
    genome: UniqueGenome,
    values: Seq[Double],
    seeder: GASeeder
  ) = (GAIntegration.scaled(genome, values) map2 FromContext { p ⇒ seeder(p.random()) })(_ ++ _)

  def populationToVariables[I](
    genome:           UniqueGenome,
    objectives:       Objectives,
    genomeValues:     I ⇒ Vector[Double],
    phenotypeValues:  I ⇒ Vector[Double],
    samplesPrototype: Val[Long],
    samples:          I ⇒ Long
  )(population: Vector[I]) =
    GAIntegration.populationToVariables[I](genome, objectives, genomeValues, phenotypeValues)(population).map {
      _ ++ Seq(Variable(samplesPrototype.toArray, population.map(samples).toArray))
    }

}

object MGOAPI {

  import cats.Monad
  import mgo.{ breeding, contexts, elitism }

  import contexts._
  import breeding._
  import elitism._
  import squants._

  trait Integration[A, V, P] {
    type M[T] = cats.data.State[S, T]
    type I
    type G
    type S

    implicit def iManifest: Manifest[I]
    implicit def gManifest: Manifest[G]
    implicit def sManifest: Manifest[S]

    def operations(a: A): Ops

    trait Ops {
      def initialState(rng: util.Random): S
      def initialGenomes(n: Int): FromContext[M[Vector[G]]]
      def buildIndividual(genome: G, phenotype: P): I
      def values(genome: G): V
      def genome(individual: I): G
      def phenotype(individual: I): P
      def genomeValues(individual: I) = values(genome(individual))
      def randomLens: monocle.Lens[S, util.Random]
      def startTimeLens: monocle.Lens[S, Long]
      def generation(s: S): Long
      def breeding(individuals: Vector[I], n: Int): M[Vector[G]]
      def elitism(individuals: Vector[I]): M[Vector[I]]
      def migrateToIsland(i: Vector[I]): Vector[I]
      def migrateFromIsland(population: Vector[I]): Vector[I]
      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] //= mgo.afterGeneration[M, I](g)
      def afterDuration(d: Time, population: Vector[I]): M[Boolean] // = mgo.afterDuration[M, I](d)
    }

  }

  trait Stochastic { self: Integration[_, _, _] ⇒
    def samples(s: I): Long
  }

  trait Profile[A] { self: Integration[A, _, _] ⇒
    def profile(a: A)(population: Vector[I]): Vector[I]
  }

}

