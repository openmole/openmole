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

import fr.iscpif.mgo
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.dsl._
import scala.util.Random
import org.openmole.core.workflow.tools._
import scalaz._
import Scalaz._
import scala.language.higherKinds

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

case class Replication[A[_]: Functor](
  seed:        Seeder                                  = Seeder.empty,
  max:         Int                                     = 100,
  reevaluate:  Double                                  = 0.2,
  aggregation: OptionalArgument[A[FitnessAggregation]] = None
)

object WorkflowIntegration {

  implicit def unionContainingIntegrationRight[T, U](implicit wi: WorkflowIntegration[U]) = new WorkflowIntegration[\&/[T, U]] {
    def apply(t: \&/[T, U]) = wi(t)
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
      def outputPrototypes = a.objectives
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

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ context(o)).toVector
    }

  def stochasticGAIntegration[AG](a: StochasticGA[AG]) =
    new EvolutionWorkflow {
      type MGOAG = AG
      def mgoAG = a.ag

      type V = Vector[Double]
      type P = Vector[Double]

      lazy val integration = a.algorithm

      def samples = Prototype[Long]("samples", namespace)

      def buildIndividual(genome: G, context: Context): I =
        operations.buildIndividual(genome, variablesToPhenotype(context))

      def inputPrototypes = a.genome.inputs.map(_.prototype) ++ a.replication.seed.prototype
      def outputPrototypes = a.objectives
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

      def variablesToPhenotype(context: Context) = a.objectives.map(o ⇒ context(o)).toVector
    }

  case class DeterministicGA[AG](
    ag:         AG,
    genome:     UniqueGenome,
    objectives: Objectives
  )(implicit val algorithm: mgo.openmole.Integration[AG, Vector[Double], Vector[Double]])

  object DeterministicGA {
    implicit def deterministicGAIntegration[AG] = new WorkflowIntegration[DeterministicGA[AG]] {
      def apply(a: DeterministicGA[AG]) = WorkflowIntegration.deterministicGAIntegration(a)
    }
  }

  case class StochasticGA[AG](
    ag:          AG,
    genome:      UniqueGenome,
    objectives:  Objectives,
    replication: Replication[Seq]
  )(
    implicit
    val algorithm: mgo.openmole.Integration[AG, Vector[Double], Vector[Double]] with mgo.openmole.Stochastic
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

  val integration: mgo.openmole.Integration[MGOAG, V, P]
  def operations = integration.operations(mgoAG)

  import integration._

  type G = integration.G
  type I = integration.I
  type S = integration.S
  type V
  type P

  type Pop = Vector[I]

  def genomeType = PrototypeType[G]
  def stateType = PrototypeType[S]
  def individualType = PrototypeType[I]

  def populationType: PrototypeType[Pop] = PrototypeType[Pop]

  def buildIndividual(genome: G, context: Context): I

  def inputPrototypes: Seq[Prototype[_]]
  def resultPrototypes: Seq[Prototype[_]]
  def outputPrototypes: Seq[Prototype[_]]

  def genomeToVariables(genome: G): FromContext[Seq[Variable[_]]]
  def populationToVariables(population: Pop): FromContext[Seq[Variable[_]]]

  // Variables
  def namespace = Namespace("evolution")
  def genomePrototype = Prototype[G]("genome", namespace)(genomeType)
  def individualPrototype = Prototype[I]("individual", namespace)(individualType)
  def populationPrototype = Prototype[Pop]("population", namespace)(populationType)
  def offspringPrototype = Prototype[Pop]("offspring", namespace)(populationType)
  def statePrototype = Prototype[S]("state", namespace)(stateType)
  def generationPrototype = Prototype[Long]("generation", namespace)
  def terminatedPrototype = Prototype[Boolean]("terminated", namespace)
}

object GAIntegration {

  def scaled(inputs: UniqueGenome, values: Seq[Double]) = FromContext { (context, rng) ⇒
    InputConverter.scaled(inputs.inputs.toList, values.toList)(context, rng)
  }

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
          p.toArray,
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
    seed:   Seeder
  ) =
    for {
      variables ← GAIntegration.scaled(genome, values)
      s ← FromContext { (_, rng) ⇒ seed(rng()) }
    } yield variables ++ s

  def populationToVariables[I](
    genome:           UniqueGenome,
    objectives:       Objectives,
    genomeValues:     I ⇒ Vector[Double],
    phenotypeValues:  I ⇒ Vector[Double],
    samplesPrototype: Prototype[Long],
    samples:          I ⇒ Long
  )(population: Vector[I]) =
    GAIntegration.populationToVariables[I](genome, objectives, genomeValues, phenotypeValues)(population).map {
      _ ++ Seq(Variable(samplesPrototype.toArray, population.map(samples).toArray))
    }

}

