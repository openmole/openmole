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

import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.{ FromContext, Condition }
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.workflow.tools.OptionalArgument
import cats._
import cats.implicits._
import mgo.evolution._
import mgo.evolution.algorithm._
import mgo.evolution.breeding._
import mgo.evolution.elitism._
import mgo.evolution.niche._
import monocle.macros.GenLens
import org.openmole.core.workflow.builder.{ DefinitionScope, ValueAssignment }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import org.openmole.plugin.method.evolution.NichedNSGA2.NichedElement
import squants.time.Time
import org.openmole.core.dsl._

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

    implicit def fromDoubleDomainToPatternAxe[D](implicit fix: Fix[D, Double]) = {
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
        mgo.evolution.niche.continuousProfile((Individual.genome composeLens CDGenome.continuousValues).get _, x, nX)

      def discreteProfile(x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile((Individual.genome composeLens CDGenome.discreteValues).get _, x)

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

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._

          val niche = DeterministicParams.niche(om.genome, om.niche).from(context)
          val res = NichedNSGA2Algorithm.result(population, niche, Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives), keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))

          genomes ++ fitness
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

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._

          val niche = DeterministicParams.niche(om.genome, om.niche).from(context)
          val (s2, elited) = NichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](niche, om.nicheSize, Genome.continuous(om.genome), ExactObjective.toFitnessFunction(om.phenotypeContent, om.objectives)) apply (s, population, candidates, rng)
          val s3 = EvolutionState.generation.modify(_ + 1)(s2)
          (s3, elited)
        }

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

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
        mgo.evolution.niche.continuousProfile((Individual.genome composeLens CDGenome.continuousValues).get _, x, nX)

      def discreteProfile(x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile((Individual.genome composeLens CDGenome.discreteValues).get _, x)

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

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[_]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean) = FromContext { p ⇒
          import p._

          val niche = StochasticParams.niche(om.genome, om.niche).from(context)
          val res = NoisyNichedNSGA2Algorithm.result(population, NoisyObjective.aggregate(om.phenotypeContent, om.objectives), niche, Genome.continuous(om.genome), onlyOldest = true, keepAll = keepAll)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
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
          NoisyNichedNSGA2Algorithm.adaptiveBreeding[S, Phenotype](n, rejectValue, om.operatorExploration, om.cloneProbability, NoisyObjective.aggregate(om.phenotypeContent, om.objectives), discrete) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext { p ⇒
            import p._

            val niche = StochasticParams.niche(om.genome, om.niche).from(context)

            val (s2, elited) = NoisyNichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](
              niche,
              om.nicheSize,
              om.historySize,
              NoisyObjective.aggregate(om.phenotypeContent, om.objectives),
              Genome.continuous(om.genome)) apply (s, population, candidates, rng)

            val s3 = EvolutionState.generation.modify(_ + 1)(s2)
            (s3, elited)
          }

        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, EvolutionState.generation)(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, EvolutionState.startTime)(s, population)

        def migrateToIsland(population: Vector[I]) = StochasticGAIntegration.migrateToIsland[I](population, CDGenome.NoisyIndividual.Individual.historyAge)
        def migrateFromIsland(population: Vector[I], state: S) = StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, CDGenome.NoisyIndividual.Individual.historyAge, CDGenome.NoisyIndividual.Individual.phenotypeHistory)
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
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    WorkflowIntegration.stochasticity(objective, stochastic.option) match {
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(exactObjectives)
        val integration: WorkflowIntegration.DeterministicGA[_] =
          new WorkflowIntegration.DeterministicGA(
            DeterministicParams(
              genome = genome,
              objectives = exactObjectives,
              phenotypeContent = phenotypeContent,
              niche = niche,
              operatorExploration = operatorExploration,
              nicheSize = nicheSize,
              reject = reject.option),
            genome,
            phenotypeContent
          )

        WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)

      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(noisyObjectives)

        val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
          StochasticParams(
            nicheSize = nicheSize,
            niche = niche,
            operatorExploration = operatorExploration,
            genome = genome,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            reject = reject.option),
          genome,
          phenotypeContent,
          stochasticValue
        )

        WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
    }

}

object ProfileEvolution {

  def apply(
    profile:      Profile.ProfileElements,
    genome:       Genome,
    objective:    Objectives,
    evaluation:   DSL,
    termination:  OMTermination,
    nicheSize:    Int                          = 10,
    stochastic:   OptionalArgument[Stochastic] = None,
    reject:       OptionalArgument[Condition]  = None,
    parallelism:  Int                          = 1,
    distribution: EvolutionPattern             = SteadyState(),
    suggestion:   Suggestion                   = Suggestion.empty,
    scope:        DefinitionScope              = "profile") = {

    EvolutionPattern.build(
      algorithm =
        Profile(
          niche = profile,
          genome = genome,
          objective = objective,
          stochastic = stochastic,
          nicheSize = nicheSize,
          reject = reject
        ),
      evaluation = evaluation,
      termination = termination,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion(genome),
      scope = scope
    )
  }

}
