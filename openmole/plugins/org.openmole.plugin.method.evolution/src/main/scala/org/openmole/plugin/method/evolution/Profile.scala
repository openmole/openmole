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

import cats._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import monocle.macros.GenLens
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import squants.time.Time
import org.openmole.core.dsl._
import org.openmole.core.dsl.`extension`._

import monocle._
import monocle.syntax.all._

object Profile {

  import org.openmole.core.keyword._

  object ToProfileElement {
    implicit def valDoubleToProfileElement: ToProfileElement[Val[Double]] =
      new ToProfileElement[Val[Double]] {
        def apply(v: Val[Double]) = IntervalDoubleProfileElement(v, 100)
      }

    implicit def valIntToProfileElement: ToProfileElement[Val[Int]] =
      new ToProfileElement[Val[Int]] {
        def apply(v: Val[Int]) = IntervalIntProfileElement(v)
      }

    implicit def inToProfileElement: ToProfileElement[In[Val[Double], Int]] =
      new ToProfileElement[In[Val[Double], Int]] {
        override def apply(t: In[Val[Double], Int]): ProfileElement = IntervalDoubleProfileElement(t.value, t.domain)
      }

    implicit def fromDoubleDomainToPatternAxe[D](implicit fix: FixDomain[D, Double]) = {
      new ToProfileElement[In[Val[Double], D]] {
        override def apply(t: In[Val[Double], D]): ProfileElement =
          FixDomainProfileElement(t.value, fix(t.domain).toVector)
      }
    }

  }

  trait ToProfileElement[-T] {
    def apply(t: T): ProfileElement
  }

  object ProfileElement {
    implicit def toProfileElement[T: ToProfileElement](t: T) = implicitly[ToProfileElement[T]].apply(t)
  }

  abstract trait ProfileElement
  case class IntervalDoubleProfileElement(v: Val[Double], n: Int) extends ProfileElement
  case class IntervalIntProfileElement(v: Val[Int]) extends ProfileElement
  case class FixDomainProfileElement(v: Val[Double], intervals: Vector[Double]) extends ProfileElement

  type ProfileElements = Seq[ProfileElement]

  object DeterministicParams {

    import CDGenome.DeterministicIndividual.Individual

    def niche(genome: Genome, profiled: Seq[ProfileElement]) = {
      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      def continuousProfile(x: Int, nX: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.continuousProfile(Focus[Individual[Phenotype]](_.genome) andThen CDGenome.continuousValues get, x, nX)

      def discreteProfile(x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile(Focus[Individual[Phenotype]](_.genome) andThen CDGenome.discreteValues get, x)

      def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.gridContinuousProfile(i ⇒ scaleContinuousValues(CDGenome.continuousValues.get(i.genome), continuous), x, intervals)

      val niches = profiled.toVector.map {
        case c: IntervalDoubleProfileElement ⇒
          val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
          continuousProfile(index, c.n)
        case c: IntervalIntProfileElement ⇒
          val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
          discreteProfile(index)
        case c: FixDomainProfileElement ⇒
          val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
          gridContinuousProfile(Genome.continuous(genome), index, c.intervals)
      }

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.DeterministicIndividual.Individual[Phenotype], Int](niches))
    }

    import CDGenome.DeterministicIndividual

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val niche = DeterministicParams.niche(om.genome, om.niche).from(context)
          val res = NichedNSGA2Algorithm.result(population, niche, Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))

          val outputValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

          genomes ++ fitness ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          mgo.evolution.algorithm.Profile.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(population: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          mgo.evolution.algorithm.Profile.adaptiveBreeding[Phenotype](n, om.operatorExploration, discrete, ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives), rejectValue) apply (s, population, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) = FromContext { p ⇒
          import p._

          val niche = DeterministicParams.niche(om.genome, om.niche).from(context)
          val (s2, elited) = NichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](niche, om.nicheSize, Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives)) apply (s, population, candidates, rng)
          val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
          val s4 = Focus[S](_.evaluated).modify(_ + evaluated)(s3)
          (s4, elited)
        }

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def migrateToIsland(population: Vector[I]) = DeterministicGAIntegration.migrateToIsland(population)
        def migrateFromIsland(population: Vector[I], state: S) = DeterministicGAIntegration.migrateFromIsland(population)
      }

    }
  }

  case class DeterministicParams(
    nicheSize:           Int,
    niche:               Seq[ProfileElement],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[ExactObjective[_]],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticParams {

    def niche(genome: Genome, profiled: Seq[ProfileElement]) = {

      def notFoundInGenome(v: Val[_]) = throw new UserBadDataError(s"Variable $v not found in the genome")

      import CDGenome.NoisyIndividual.Individual

      def continuousProfile(x: Int, nX: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.continuousProfile((Focus[Individual[Phenotype]](_.genome) composeLens CDGenome.continuousValues).get _, x, nX)

      def discreteProfile(x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile((Focus[Individual[Phenotype]](_.genome) composeLens CDGenome.discreteValues).get _, x)

      def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.gridContinuousProfile(i ⇒ scaleContinuousValues(CDGenome.continuousValues.get(i.genome), continuous), x, intervals)

      val niches =
        profiled.toVector.map {
          case c: IntervalDoubleProfileElement ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            continuousProfile(index, c.n)
          case c: IntervalIntProfileElement ⇒
            val index = Genome.discreteIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            discreteProfile(index)
          case c: FixDomainProfileElement ⇒
            val index = Genome.continuousIndex(genome, c.v).getOrElse(notFoundInGenome(c.v))
            gridContinuousProfile(Genome.continuous(genome), index, c.intervals)
        }

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.NoisyIndividual.Individual[Phenotype], Int](niches))
    }

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticParams) = new Ops {
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val niche = StochasticParams.niche(om.genome, om.niche).from(context)
          val res = NoisyNichedNSGA2Algorithm.result(population, NoisyObjective.aggregate(om.phenotypeContent, om.objectives).from(context), niche, Genome.continuous(om.genome), onlyOldest = true, keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
          val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)

          val outputValues = if (includeOutputs) StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory)) else Seq()

          genomes ++ fitness ++ Seq(samples) ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyNichedNSGA2Algorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._

          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyNichedNSGA2Algorithm.adaptiveBreeding[S, Phenotype](n, rejectValue, om.operatorExploration, om.cloneProbability, NoisyObjective.aggregate(om.phenotypeContent, om.objectives).from(context), discrete) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) =
          FromContext { p ⇒
            import p._

            val niche = StochasticParams.niche(om.genome, om.niche).from(context)

            val (s2, elited) = NoisyNichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](
              niche,
              om.nicheSize,
              om.historySize,
              NoisyObjective.aggregate(om.phenotypeContent, om.objectives).from(context),
              Genome.continuous(om.genome)) apply (s, population, candidates, rng)

            val s3 = Focus[S](_.generation).modify(_ + 1)(s2)
            val s4 = Focus[S](_.evaluated).modify(_ + evaluated)(s3)
            (s4, elited)
          }

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, Focus[I](_.historyAge))
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, Focus[I](_.historyAge), Focus[I](_.phenotypeHistory))
      }

    }
  }

  case class StochasticParams(
    nicheSize:           Int,
    niche:               Seq[ProfileElement],
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[NoisyObjective[_]],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition])

  def apply[P](
    niche:      Seq[ProfileElement],
    genome:     Genome,
    objective:  Objectives,
    nicheSize:  Int,
    outputs:    Seq[Val[_]]                  = Seq(),
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective, stochastic.option) match {
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicParams(
            genome = genome,
            objectives = exactObjectives,
            phenotypeContent = phenotypeContent,
            niche = niche,
            operatorExploration = EvolutionWorkflow.operatorExploration,
            nicheSize = nicheSize,
            reject = reject.option),
          genome,
          phenotypeContent,
          validate = Objectives.validate(objective, outputs)
        )
      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate = {
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)
        }

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticParams(
            nicheSize = nicheSize,
            niche = niche,
            operatorExploration = EvolutionWorkflow.operatorExploration,
            genome = genome,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          genome,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )
    }

}

import EvolutionWorkflow._

object ProfileEvolution {

  implicit def method: ExplorationMethod[ProfileEvolution, EvolutionWorkflow] =
    p ⇒ {
      val container = EvolutionPattern.build(
        algorithm =
          Profile(
            niche = p.profile,
            genome = p.genome,
            objective = p.objective,
            outputs = p.evaluation.outputs,
            stochastic = p.stochastic,
            nicheSize = p.nicheSize,
            reject = p.reject
          ),
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(p.genome),
        scope = p.scope
      )

      container hook (p.hooks.map(_(container.method, p.scope)): _*)
    }

  implicit def patternContainer: ExplorationMethodSetter[ProfileEvolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)
  implicit def hookContainer: ExplorationMethodSetter[ProfileEvolution, SavePopulationHook.Parameter[_]] = (e, p) ⇒ e.copy(hooks = e.hooks ++ Seq(p))

}

import monocle.macros._

@Lenses case class ProfileEvolution(
  profile:      Profile.ProfileElements,
  genome:       Genome,
  objective:    Objectives,
  evaluation:   DSL,
  termination:  OMTermination,
  nicheSize:    Int                                  = 10,
  stochastic:   OptionalArgument[Stochastic]         = None,
  reject:       OptionalArgument[Condition]          = None,
  parallelism:  Int                                  = EvolutionWorkflow.parallelism,
  distribution: EvolutionPattern                     = SteadyState(),
  suggestion:   Suggestion                           = Suggestion.empty,
  scope:        DefinitionScope                      = "profile",
  hooks:        Seq[SavePopulationHook.Parameter[_]] = Seq())