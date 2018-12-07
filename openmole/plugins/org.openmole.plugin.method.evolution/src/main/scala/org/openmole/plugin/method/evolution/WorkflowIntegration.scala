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
import shapeless.TypeCase

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

case class Stochastic(
  seed:         GASeeder = GASeeder.empty,
  replications: Int      = 100,
  reevaluate:   Double   = 0.2
)

object WorkflowIntegration {

  implicit def hlistContainingIntegration[H <: shapeless.HList, U](implicit hwi: WorkflowIntegrationSelector[H, U]) = new WorkflowIntegration[H] {
    def apply(h: H) = hwi.selected(hwi(h))
  }

  def deterministicGAIntegration[AG](a: DeterministicGA[AG]): EvolutionWorkflow =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = (Vector[Double], Vector[Int])
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context), context)

      def inputPrototypes = Genome.toVals(a.genome)
      def objectivePrototypes = a.objectives.map(Objective.prototype)
      def resultPrototypes = (inputPrototypes ++ objectivePrototypes).distinct

      def genomeToVariables(genome: G): FromContext[Vector[Variable[_]]] = {
        val (cs, is) = operations.genomeValues(genome)
        Genome.toVariables(a.genome, cs, is, scale = true)
      }

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ Objective.toDouble(o, context)).toVector
    }

  def stochasticGAIntegration[AG](a: StochasticGA[AG]): EvolutionWorkflow =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = (Vector[Double], Vector[Int])
      type P = Vector[Any]

      lazy val integration = a.algorithm

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context), context)

      def inputPrototypes = Genome.toVals(a.genome) ++ a.replication.seed.prototype
      def objectivePrototypes = a.objectives.map(Objective.prototype)

      def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]] = {
        val (continuous, discrete) = operations.genomeValues(genome)
        val seeder = a.replication.seed
        (Genome.toVariables(a.genome, continuous, discrete, scale = true) map2 FromContext { p ⇒ seeder(p.random()) })(_ ++ _)
      }

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ Objective.prototype(o)).map(context.apply(_)).toVector
    }

  case class DeterministicGA[AG](
    ag:         AG,
    genome:     Genome,
    objectives: Seq[ExactObjective[_]]
  )(implicit val algorithm: MGOAPI.Integration[AG, (Vector[Double], Vector[Int]), Vector[Double]])

  object DeterministicGA {
    implicit def deterministicGAIntegration[AG]: WorkflowIntegration[DeterministicGA[AG]] = new WorkflowIntegration[DeterministicGA[AG]] {
      def apply(a: DeterministicGA[AG]) = WorkflowIntegration.deterministicGAIntegration(a)
    }

    def toEvolutionWorkflow(a: DeterministicGA[_]): EvolutionWorkflow = WorkflowIntegration.deterministicGAIntegration(a)
  }

  case class StochasticGA[AG](
    ag:          AG,
    genome:      Genome,
    objectives:  Seq[NoisyObjective[_]],
    replication: Stochastic
  )(
    implicit
    val algorithm: MGOAPI.Integration[AG, (Vector[Double], Vector[Int]), Vector[Any]]
  )

  object StochasticGA {
    implicit def stochasticGAIntegration[AG]: WorkflowIntegration[StochasticGA[AG]] = new WorkflowIntegration[StochasticGA[AG]] {
      override def apply(a: StochasticGA[AG]) = WorkflowIntegration.stochasticGAIntegration(a)
    }

    def toEvolutionWorkflow(a: StochasticGA[_]): EvolutionWorkflow = WorkflowIntegration.stochasticGAIntegration(a)
  }

}

trait WorkflowIntegration[T] {
  def apply(t: T): EvolutionWorkflow
}

object EvolutionWorkflow {
  implicit def isWorkflowIntegration: WorkflowIntegration[EvolutionWorkflow] = new WorkflowIntegration[EvolutionWorkflow] {
    def apply(t: EvolutionWorkflow) = t
  }

  //case class EvolutionState[S](s: S, island)
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

  type Pop = Array[I]

  def genomeType = ValType[G]
  def stateType = ValType[S]
  def individualType = ValType[I]

  def populationType: ValType[Pop] = ValType[Pop]

  def buildIndividual(genome: G, context: Context): I

  def inputPrototypes: Seq[Val[_]]
  def objectivePrototypes: Seq[Val[_]]

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

    variables.map {
      v ⇒
        genome.zipWithIndex.map {
          case (g, i) ⇒ Genome.toArrayVariable(g, v.map(_(i).value))
        }.toVector
    }
  }

  def objectivesOfPopulationToVariables[I](objectives: Seq[Objective[_]], phenotypeValues: Vector[Vector[Double]]): FromContext[Vector[Variable[_]]] =
    objectives.toVector.zipWithIndex.map {
      case (p, i) ⇒
        Variable(
          Objective.prototype(p).withType[Array[Double]],
          phenotypeValues.map(_(i)).toArray
        )
    }

}

object DeterministicGAIntegration {
  def migrateToIsland(population: Vector[mgo.algorithm.CDGenome.DeterministicIndividual.Individual]) = population

}

object StochasticGAIntegration {

  //  def aggregateVector[P](stochastic: Stochastic[P], values: Vector[Vector[P]]): Vector[Double] =
  //    (stochastic.aggregation.option, stochastic) match {
  //      case (Some(aggs), _)               ⇒ (values.transpose zip aggs).map { case (p, a) ⇒ a(p) }
  //      case (_, Stochastic.caseDouble(s)) ⇒ values.transpose.map(_.median)
  //      case _                             ⇒ ???
  //    }

  def migrateToIsland[P](population: Vector[mgo.algorithm.CDGenome.NoisyIndividual.Individual[P]]) = population.map(_.copy(historyAge = 0))

}

object MGOAPI {

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
      def buildIndividual(genome: G, phenotype: P, context: Context): I

      def genomeValues(genome: G): V
      def buildGenome(values: V): G
      def buildGenome(context: Vector[Variable[_]]): FromContext[G]

      def randomLens: monocle.Lens[S, util.Random]
      def startTimeLens: monocle.Lens[S, Long]
      def generation(s: S): Long
      def breeding(individuals: Vector[I], n: Int): FromContext[M[Vector[G]]]
      def elitism(individuals: Vector[I]): FromContext[M[Vector[I]]]

      def migrateToIsland(i: Vector[I]): Vector[I]
      def migrateFromIsland(population: Vector[I], state: S): Vector[I]

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean]
      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean]

      def result(population: Vector[I], state: S): FromContext[Seq[Variable[_]]]
    }

  }

  def paired[G, C, D](continuous: G ⇒ C, discrete: G ⇒ D) = (g: G) ⇒ (continuous(g), discrete(g))

}

