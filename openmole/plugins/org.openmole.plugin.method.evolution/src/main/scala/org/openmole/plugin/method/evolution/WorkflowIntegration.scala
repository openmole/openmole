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

      type V = (Vector[Double], Vector[Int])
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context))

      def inputPrototypes = Genome.vals(a.genome)
      def objectives = a.objectives
      def resultPrototypes = (inputPrototypes ++ outputPrototypes).distinct

      def genomeToVariables(genome: G): FromContext[Vector[Variable[_]]] = {
        val (cs, is) = operations.genomeValues(genome)
        Genome.toVariables(a.genome, cs, is, scale = true)
      }

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ o.fromContext(context)).toVector
    }

  def stochasticGAIntegration[AG](a: StochasticGA[AG]) =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = (Vector[Double], Vector[Int])
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context))

      def inputPrototypes = Genome.vals(a.genome) ++ a.replication.seed.prototype
      def objectives = a.objectives

      def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] = {
        val (continuous, discrete) = operations.genomeValues(genome)
        val seeder = a.replication.seed
        (Genome.toVariables(a.genome, continuous, discrete, scale = true) map2 FromContext { p ⇒ seeder(p.random()) })(_ ++ _)
      }

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ o.fromContext(context)).toVector
    }

  case class DeterministicGA[AG](
    ag:         AG,
    genome:     Genome,
    objectives: Objectives
  )(implicit val algorithm: MGOAPI.Integration[AG, (Vector[Double], Vector[Int]), Vector[Double]])

  object DeterministicGA {
    implicit def deterministicGAIntegration[AG] = new WorkflowIntegration[DeterministicGA[AG]] {
      def apply(a: DeterministicGA[AG]) = WorkflowIntegration.deterministicGAIntegration(a)
    }
  }

  case class StochasticGA[AG](
    ag:          AG,
    genome:      Genome,
    objectives:  Objectives,
    replication: Stochastic[Seq]
  )(
    implicit
    val algorithm: MGOAPI.Integration[AG, (Vector[Double], Vector[Int]), Vector[Double]]
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
  def objectives: Seq[Objective]
  def outputPrototypes: Seq[Val[Double]] = objectives.map(_.prototype.withType[Double])

  def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]]

  // Variables
  import GAIntegration.namespace
  def genomePrototype = Val[G]("genome", namespace)(genomeType)
  def individualPrototype = Val[I]("individual", namespace)(individualType)
  def populationPrototype = Val[Pop]("population", namespace)(populationType)
  def offspringPrototype = Val[Pop]("offspring", namespace)(populationType)
  def statePrototype = Val[S]("state", namespace)(stateType)
  def generationPrototype = Val[Long]("generation", namespace)
  def terminatedPrototype = Val[Boolean]("terminated", namespace)
}

object GAIntegration {

  def namespace = Namespace("evolution")
  def samples = Val[Int]("samples", namespace)

  def genomesOfPopulationToVariables[I](
    genome: Genome,
    values: Vector[(Vector[Double], Vector[Int])],
    scale:  Boolean): FromContext[Vector[Variable[_]]] = {

    val variables =
      values.traverse[FromContext, Vector[Variable[_]]] {
        case (continuous, discrete) ⇒ Genome.toVariables(genome, continuous, discrete, scale)
      }

    import Genome.GenomeBound

    def toArrayVariable(genomeBound: GenomeBound, value: Seq[Any]) = genomeBound match {
      case b: GenomeBound.ScalarDouble ⇒
        Variable(b.v.toArray, value.map(_.asInstanceOf[Double]).toArray[Double])
      case b: GenomeBound.ScalarInt ⇒
        Variable(b.v.toArray, value.map(_.asInstanceOf[Int]).toArray[Int])
      case b: GenomeBound.SequenceOfDouble ⇒
        Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Double]]).toArray[Array[Double]])
      case b: GenomeBound.SequenceOfInt ⇒
        Variable(b.v.toArray, value.map(_.asInstanceOf[Array[Int]]).toArray[Array[Int]])
      case b: GenomeBound.Enumeration[_] ⇒
        val array = b.v.`type`.manifest.newArray(value.size)
        value.zipWithIndex.foreach { case (v, i) ⇒ java.lang.reflect.Array.set(array, i, v) }
        Variable.unsecure(b.v.toArray, array)
    }

    variables.map {
      v ⇒
        genome.zipWithIndex.map {
          case (g, i) ⇒ toArrayVariable(g, v.map(_(i).value))
        }.toVector
    }
  }

  def objectivesOfPopulationToVariables[I](objectives: Objectives, phenotypeValues: Vector[Vector[Double]]): FromContext[Vector[Variable[_]]] =
    objectives.toVector.zipWithIndex.map {
      case (p, i) ⇒
        Variable(
          p.prototype.withType[Array[Double]],
          phenotypeValues.map(_(i)).toArray
        )
    }

}

object StochasticGAIntegration {

  def aggregateVector(aggregation: Option[Seq[FitnessAggregation]], values: Vector[Vector[Double]]): Vector[Double] =
    aggregation match {
      case Some(aggs) ⇒ (values.transpose zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ values.transpose.map(_.median)
    }

  def migrateToIsland(population: Vector[mgo.algorithm.CDGenome.NoisyIndividual.Individual]) = population.map(_.copy(historyAge = 0))
  def migrateFromIsland(population: Vector[mgo.algorithm.CDGenome.NoisyIndividual.Individual], historySize: Int) =
    population.filter(_.historyAge != 0).map {
      i ⇒ mgo.algorithm.CDGenome.NoisyIndividual.Individual.fitnessHistory.modify(_.take(math.min(i.historyAge, historySize).toInt))(i)
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
      def genomeValues(genome: G): V
      def genome(individual: I): G
      def phenotype(individual: I): P
      def randomLens: monocle.Lens[S, util.Random]
      def startTimeLens: monocle.Lens[S, Long]
      def generation(s: S): Long
      def breeding(individuals: Vector[I], n: Int): FromContext[M[Vector[G]]]
      def elitism(individuals: Vector[I]): FromContext[M[Vector[I]]]
      def migrateToIsland(i: Vector[I]): Vector[I]
      def migrateFromIsland(population: Vector[I]): Vector[I]
      def afterGeneration(g: Long, population: Vector[I]): M[Boolean]
      def afterDuration(d: Time, population: Vector[I]): M[Boolean]
      def result(population: Vector[I]): FromContext[Seq[Variable[_]]]
    }

  }

  def paired[G, C, D](continuous: G ⇒ C, discrete: G ⇒ D) = (g: G) ⇒ (continuous(g), discrete(g))

}

