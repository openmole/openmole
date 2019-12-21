/*
 * Copyright (C) 2015 Romain Reuillon
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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.FromContext
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats._
import cats.data._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm.NoisyPSE.PSEState
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import mgo.tools.CanBeNaN
import monocle.macros.GenLens
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.keyword.{ In, Under }
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools.OptionalArgument
import org.openmole.plugin.method.evolution.Genome.{ GenomeBound, Suggestion }
import org.openmole.plugin.method.evolution.Objective.{ ToExactObjective, ToNoisyObjective }
import org.openmole.tool.types.ToDouble
import squants.time.Time

import scala.language.higherKinds
import scala.reflect.ClassTag

object PSEAlgorithm {

  import mgo.tools._
  import monocle.macros.{ GenLens, Lenses }

  import scala.language.higherKinds
  import cats.implicits._
  import mgo.evolution.algorithm._

  import GenomeVectorDouble._
  import CDGenome._

  @Lenses case class Individual[P](
    genome:        CDGenome.Genome,
    phenotype:     P,
    foundedIsland: Boolean         = false)

  case class Result(continuous: Vector[Double], discrete: Vector[Int], pattern: Vector[Int], phenotype: Vector[Double])

  def result[P](population: Vector[Individual[P]], continuous: Vector[C], pattern: Vector[Double] ⇒ Vector[Int], phenotype: P ⇒ Vector[Double]) =
    population.map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Individual.genome composeLens discreteValues get i,
        pattern(phenotype(i.phenotype)),
        phenotype(i.phenotype))
    }

  def buildIndividual[P](g: CDGenome.Genome, p: P) = Individual(g, p)
  // def vectorPhenotype = Individual.phenotype composeLens arrayToVectorLens

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

  def adaptiveBreeding[S, P](
    lambda:              Int,
    reject:              Option[CDGenome.Genome ⇒ Boolean],
    operatorExploration: Double,
    discrete:            Vector[D],
    pattern:             P ⇒ Vector[Int],
    hitmap:              monocle.Lens[S, HitMap]) =
    PSEOperations.adaptiveBreeding[S, Individual[P], CDGenome.Genome](
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      Individual.phenotype[P].get _ andThen pattern,
      buildGenome,
      lambda,
      reject,
      operatorExploration,
      hitmap)

  def elitism[S, P: CanBeNaN](pattern: P ⇒ Vector[Int], continuous: Vector[C], hitmap: monocle.Lens[S, HitMap]) =
    PSEOperations.elitism[S, Individual[P], P](
      i ⇒ values(Individual.genome.get(i), continuous),
      Individual.phenotype[P].get,
      pattern,
      hitmap)

  def expression[P](phenotype: (Vector[Double], Vector[Int]) ⇒ P, continuous: Vector[C]): CDGenome.Genome ⇒ Individual[P] =
    deterministic.expression[CDGenome.Genome, P, Individual[P]](
      values(_, continuous),
      buildIndividual,
      phenotype)

}

object NoisyPSEAlgorithm {

  import mgo.evolution._
  import breeding._
  import monocle.macros._
  import algorithm._
  import algorithm.CDGenome._

  @Lenses case class Individual[P](
    genome:           CDGenome.Genome,
    historyAge:       Long,
    phenotypeHistory: Array[P])

  def buildIndividual[P: Manifest](genome: CDGenome.Genome, phenotype: P) = Individual(genome, 1, Array(phenotype))
  def vectorPhenotype[P: Manifest] = Individual.phenotypeHistory[P] composeLens arrayToVectorLens

  def initialGenomes(lambda: Int, continuous: Vector[C], discrete: Vector[D], reject: Option[Genome ⇒ Boolean], rng: scala.util.Random) =
    CDGenome.initialGenomes(lambda, continuous, discrete, reject, rng)

  def adaptiveBreeding[S, P: Manifest](
    lambda:              Int,
    reject:              Option[Genome ⇒ Boolean],
    operatorExploration: Double,
    cloneProbability:    Double,
    aggregation:         Vector[P] ⇒ Vector[Double],
    discrete:            Vector[D],
    pattern:             Vector[Double] ⇒ Vector[Int],
    hitmap:              monocle.Lens[S, HitMap]) =
    NoisyPSEOperations.adaptiveBreeding[S, Individual[P], Genome](
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      vectorPhenotype.get _ andThen aggregation andThen pattern,
      buildGenome,
      lambda,
      reject,
      operatorExploration,
      cloneProbability,
      hitmap)

  def elitism[S, P: Manifest: CanBeNaN](
    pattern:     Vector[Double] ⇒ Vector[Int],
    aggregation: Vector[P] ⇒ Vector[Double],
    historySize: Int,
    continuous:  Vector[C],
    hitmap:      monocle.Lens[S, HitMap]) =
    NoisyPSEOperations.elitism[S, Individual[P], P](
      i ⇒ values(Individual.genome.get(i), continuous),
      vectorPhenotype[P],
      aggregation,
      pattern,
      Individual.historyAge,
      historySize,
      hitmap)

  def expression[P: Manifest](fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ P, continuous: Vector[C]): (util.Random, Genome) ⇒ Individual[P] =
    noisy.expression[CDGenome.Genome, Individual[P], P](
      values(_, continuous),
      buildIndividual)(fitness)

  def aggregate[P: Manifest](i: Individual[P], aggregation: Vector[P] ⇒ Vector[Double], pattern: Vector[Double] ⇒ Vector[Int], continuous: Vector[C]) =
    (
      scaleContinuousValues(continuousValues.get(i.genome), continuous),
      Individual.genome composeLens discreteValues get i,
      aggregation(vectorPhenotype.get(i)),
      (vectorPhenotype.get _ andThen aggregation andThen pattern)(i),
      Individual.phenotypeHistory.get(i).size)

  case class Result(continuous: Vector[Double], discrete: Vector[Int], phenotype: Vector[Double], pattern: Vector[Int], replications: Int)

  def result[P: Manifest](
    population:  Vector[Individual[P]],
    aggregation: Vector[P] ⇒ Vector[Double],
    pattern:     Vector[Double] ⇒ Vector[Int],
    continuous:  Vector[C]) =
    population.map {
      i ⇒
        val (c, d, f, p, r) = aggregate(i, aggregation, pattern, continuous)
        Result(c, d, f, p, r)
    }

}

object PSE {

  implicit def anyCanBeNan: CanBeNaN[Any] = new CanBeNaN[Any] {
    override def isNaN(t: Any): Boolean = t match {
      case x: Double ⇒ x.isNaN
      case x: Float  ⇒ x.isNaN
      case x         ⇒ false
    }
  }

  implicit def arrayCanBeNaN[T](implicit cbn: CanBeNaN[T]) = new CanBeNaN[Array[T]] {
    override def isNaN(t: Array[T]): Boolean = t.exists(cbn.isNaN)
  }

  case class DeterministicParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double,
    reject:              Option[Condition]
  )

  object DeterministicParams {

    import mgo.evolution.algorithm.{ PSE ⇒ _, _ }
    import cats.data._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Array[Any]] { api ⇒
      type G = CDGenome.Genome
      type I = PSEAlgorithm.Individual[Array[Any]]
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops {

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = PSEAlgorithm.buildIndividual(genome, phenotype)

        def initialState = EvolutionState[HitMapState](s = Map())

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = PSEAlgorithm.result[Array[Any]](population, Genome.continuous(om.genome).from(context), om.pattern, ExactObjective.toFitnessFunction(om.objectives))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          PSEAlgorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        private def pattern(p: Array[Any]) = om.pattern(ExactObjective.toFitnessFunction(om.objectives)(p))

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          PSEAlgorithm.adaptiveBreeding[S, Array[Any]](
            n,
            rejectValue,
            om.operatorExploration,
            discrete,
            pattern,
            EvolutionState.s)(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) = PSEAlgorithm.elitism[S, Array[Any]](pattern, continuous, EvolutionState.s) apply (s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) = population.map(PSEAlgorithm.Individual.foundedIsland.set(true))

        def migrateFromIsland(population: Vector[I], state: S) =
          population.filter(i ⇒ !PSEAlgorithm.Individual.foundedIsland.get(i)).
            map(PSEAlgorithm.Individual.foundedIsland.set(false))
      }

    }
  }

  case class StochasticParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticParams {

    import mgo.evolution.algorithm.{ PSE ⇒ _, NoisyPSE ⇒ _, _ }
    import cats.data._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Array[Any]] { api ⇒
      type G = CDGenome.Genome
      type I = NoisyPSEAlgorithm.Individual[Array[Any]]
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticParams) = new Ops {

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = Genome.fromVariables(vs, om.genome).map(buildGenome)

        def buildIndividual(genome: G, phenotype: Array[Any], context: Context) = NoisyPSEAlgorithm.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[HitMapState](s = Map())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._
          import org.openmole.core.context._

          val res = NoisyPSEAlgorithm.result(population, NoisyObjective.aggregate(om.objectives), om.pattern, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome).from(context)
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyPSEAlgorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome).from(context)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyPSEAlgorithm.adaptiveBreeding[S, Array[Any]](
            n,
            rejectValue,
            om.operatorExploration,
            om.cloneProbability,
            NoisyObjective.aggregate(om.objectives),
            discrete,
            om.pattern,
            EvolutionState.s) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          Genome.continuous(om.genome).map { continuous ⇒
            val (s2, elited) =
              NoisyPSEAlgorithm.elitism[S, Array[Any]](
                om.pattern,
                NoisyObjective.aggregate(om.objectives),
                om.historySize,
                continuous,
                EvolutionState.s) apply (s, population, candidates, rng)
            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def migrateToIsland(population: Vector[I]) =
          StochasticGAIntegration.migrateToIsland[I](population, NoisyPSEAlgorithm.Individual.historyAge)

        def migrateFromIsland(population: Vector[I], state: S) =
          StochasticGAIntegration.migrateFromIsland[I, Array[Any]](population, NoisyPSEAlgorithm.Individual.historyAge, NoisyPSEAlgorithm.Individual.phenotypeHistory)

      }

    }
  }

  object PatternAxe {
    implicit def fromInExactToPatternAxe[T, D](v: In[T, D])(implicit fix: Fix[D, Double], te: ToExactObjective[T]) = PatternAxe(te.apply(v.value), fix(v.domain).toVector)
    implicit def fromInNoisyToPatternAxe[T, D](v: In[T, D])(implicit fix: Fix[D, Double], te: ToNoisyObjective[T]) = PatternAxe(te.apply(v.value), fix(v.domain).toVector)

    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: Fix[D, Long]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective[_], scale: Vector[Double])

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe],
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ) =
    WorkflowIntegration.stochasticity(objectives.map(_.p), stochastic.option) match {
      case None ⇒
        val exactObjectives = objectives.map(o ⇒ Objective.toExact(o.p))

        val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
          DeterministicParams(
            mgo.evolution.niche.irregularGrid(objectives.map(_.scale).toVector),
            genome,
            exactObjectives,
            operatorExploration,
            reject = reject.option),
          genome,
          exactObjectives)(DeterministicParams.integration)

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
      case Some(stochasticValue) ⇒
        val noisyObjectives = objectives.map(o ⇒ Objective.toNoisy(o.p))

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(
            pattern = mgo.evolution.niche.irregularGrid(objectives.map(_.scale).toVector),
            genome = genome,
            objectives = noisyObjectives,
            historySize = stochasticValue.replications,
            cloneProbability = stochasticValue.reevaluate,
            operatorExploration = operatorExploration,
            reject = reject.option),
          genome,
          noisyObjectives,
          stochasticValue)(StochasticParams.integration)

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object PSEEvolution {

  import org.openmole.core.dsl.DSL

  def apply(
    genome:       Genome,
    objectives:   Seq[PSE.PatternAxe],
    evaluation:   DSL,
    termination:  OMTermination,
    stochastic:   OptionalArgument[Stochastic] = None,
    reject:       OptionalArgument[Condition]  = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "pse") =
    EvolutionPattern.build(
      algorithm =
        PSE(
          genome = genome,
          objectives = objectives,
          stochastic = stochastic,
          reject = reject
        ),
      evaluation = evaluation,
      termination = termination,
      stochastic = stochastic,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion(genome),
      scope = scope
    )

}
