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
import org.openmole.core.setter.{ DefinitionScope, ValueAssignment }
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

  object ToProfileElement:
    implicit def valDoubleToProfileElement: ToProfileElement[Val[Double]] = v => IntervalDoubleProfileElement(v, 100)
    implicit def valIntToProfileElement: ToProfileElement[Val[Int]] = v => IntervalIntProfileElement(v)
    implicit def inToProfileElement: ToProfileElement[In[Val[Double], Int]] = t => IntervalDoubleProfileElement(t.value, t.domain)
    implicit def fromDoubleDomainToPatternAxe[D](implicit fix: FixDomain[D, Double]): ToProfileElement[In[Val[Double], D]] = t => FixDomainProfileElement(t.value, fix(t.domain).domain.toVector)


  trait ToProfileElement[-T]:
    def apply(t: T): ProfileElement

  object ProfileElement {
    implicit def toProfileElement[T: ToProfileElement](t: T): ProfileElement = implicitly[ToProfileElement[T]].apply(t)
  }

  abstract trait ProfileElement
  case class IntervalDoubleProfileElement(v: Val[Double], n: Int) extends ProfileElement
  case class IntervalIntProfileElement(v: Val[Int]) extends ProfileElement
  case class FixDomainProfileElement(v: Val[Double], intervals: Vector[Double]) extends ProfileElement

  type ProfileElements = Seq[ProfileElement] | ProfileElement


  def indexesOfProfiledTry(profiled: Seq[ProfileElement], genome: Genome) =
    def notFoundInGenome(v: Val[?]) = new UserBadDataError(s"Variable \"$v\" has not been found in the genome, you should add it to the genome to be able to profile it.")
    def toTry[T](o: Option[T], v: Val[?]): util.Try[T] = o.map(util.Success.apply).getOrElse(util.Failure(notFoundInGenome(v)))

    profiled.toVector.map:
      case c: IntervalDoubleProfileElement => toTry(Genome.continuousIndex(genome, c.v), c.v)
      case c: IntervalIntProfileElement => toTry(Genome.discreteIndex(genome, c.v), c.v)
      case c: FixDomainProfileElement => toTry(Genome.continuousIndex(genome, c.v), c.v)

  def indexesOfProfiled(profiled: Seq[ProfileElement], genome: Genome) = indexesOfProfiledTry(profiled, genome).map(_.get)

  object DeterministicProfile {

    import CDGenome.DeterministicIndividual.Individual

    def niche(genome: Genome, profiled: Seq[ProfileElement]) =

      def continuousProfile(continuous: Vector[C], x: Int, nX: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.continuousProfile(Focus[Individual[Phenotype]](_.genome) andThen CDGenome.continuousVectorValues(continuous) get, x, nX)

      def discreteProfile(discrete: Vector[D], x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile(Focus[Individual[Phenotype]](_.genome) andThen CDGenome.discreteVectorValues(discrete) get, x)

      def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.gridContinuousProfile(i => scaleContinuousValues(CDGenome.continuousValues(continuous).get(i.genome), continuous).toVector, x, intervals)

      val niches = (profiled.toVector zip indexesOfProfiled(profiled, genome)).map:
        case (c: IntervalDoubleProfileElement, index) => continuousProfile(genome.continuous, index, c.n)
        case (c: IntervalIntProfileElement, index) => discreteProfile(genome.discrete, index)
        case (c: FixDomainProfileElement, index) => gridContinuousProfile(genome.continuous, index, c.intervals)

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.DeterministicIndividual.Individual[Phenotype], Int](niches))

    import CDGenome.DeterministicIndividual

    given MGOAPI.Integration[DeterministicProfile, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[DeterministicProfile, (IArray[Double], IArray[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicProfile) = new Ops:
        override def metadata(state: S, saveOption: SaveOption): EvolutionMetadata =
          EvolutionMetadata.Profile(
            genome = MetadataGeneration.genomeData(om.genome),
            objective = om.objectives.map(MetadataGeneration.objectiveData),
            generation = generationLens.get(state),
            saveOption = saveOption
          )

        def startTimeLens = Focus[S](_.startTime)
        def generationLens = Focus[S](_.generation)
        def evaluatedLens = Focus[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)
        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p._

            val niche = DeterministicProfile.niche(om.genome, om.niche).from(context)
            val res = NichedNSGA2Algorithm.result(population, niche, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            val continuous = om.genome.continuous
            val discrete = om.genome.discrete
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            mgo.evolution.algorithm.Profile.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(population: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._
            val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
            mgo.evolution.algorithm.Profile.adaptiveBreeding[Phenotype](n, om.operatorExploration, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), rejectValue) apply (s, population, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p =>
            import p._

            val niche = DeterministicProfile.niche(om.genome, om.niche).from(context)
            NichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](niche, om.nicheSize, om.genome.continuous, om.genome.discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)


    }
  }

  case class DeterministicProfile(
    nicheSize:           Int,
    niche:               Seq[ProfileElement],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticProfile {

    def niche(genome: Genome, profiled: Seq[ProfileElement]) =
      import CDGenome.NoisyIndividual.Individual

      def continuousProfile(continuous: Vector[C], x: Int, nX: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.continuousProfile((Focus[Individual[Phenotype]](_.genome) composeLens CDGenome.continuousVectorValues(continuous)).get, x, nX)

      def discreteProfile(discrete: Vector[D], x: Int): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.discreteProfile((Focus[Individual[Phenotype]](_.genome) composeLens CDGenome.discreteVectorValues(discrete)).get, x)

      def gridContinuousProfile(continuous: Vector[C], x: Int, intervals: Vector[Double]): Niche[Individual[Phenotype], Int] =
        mgo.evolution.niche.gridContinuousProfile(i => scaleContinuousValues(CDGenome.continuousValues(continuous).get(i.genome), continuous).toVector, x, intervals)

      val niches =
        (profiled.toVector zip indexesOfProfiled(profiled, genome)).map:
          case (c: IntervalDoubleProfileElement, index) => continuousProfile(genome.continuous, index, c.n)
          case (c: IntervalIntProfileElement, index) => discreteProfile(genome.discrete, index)
          case (c: FixDomainProfileElement, index) => gridContinuousProfile(genome.continuous, index, c.intervals)

      FromContext.value(mgo.evolution.niche.sequenceNiches[CDGenome.NoisyIndividual.Individual[Phenotype], Int](niches))

    given MGOAPI.Integration[StochasticProfile, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[StochasticProfile, (IArray[Double], IArray[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticProfile) = new Ops:
        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues(om.genome.continuous).get, CDGenome.discreteValues(om.genome.discrete).get)(genome)
        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(om.genome.discrete)(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[Unit](s = ())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p =>
            import p._

            val niche = StochasticProfile.niche(om.genome, om.niche).from(context)
            val res = NoisyNichedNSGA2Algorithm.result(population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), niche, om.genome.continuous, om.genome.discrete, onlyOldest = true, keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues = if (includeOutputs) StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory)) else Seq()

            genomes ++ fitness ++ Seq(samples, generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p =>
          import p._
          val continuous = om.genome.continuous
          val discrete = om.genome.discrete
          val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
          NoisyNichedNSGA2Algorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p =>
          import p._

          val rejectValue = om.reject.map(f => GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, CDGenome.discreteValues(om.genome.discrete).get).from(context))
          NoisyNichedNSGA2Algorithm.adaptiveBreeding[S, Phenotype](n, rejectValue, om.operatorExploration, om.cloneProbability, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), om.genome.continuous, om.genome.discrete) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext { p =>
            import p._

            val niche = StochasticProfile.niche(om.genome, om.niche).from(context)

            NoisyNichedNSGA2Algorithm.elitism[S, Vector[Int], Phenotype](
              niche,
              om.nicheSize,
              om.historySize,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              om.genome.continuous,
              om.genome.discrete) apply (s, population, candidates, rng)

          }

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), state)

    }
  }

  case class StochasticProfile(
    nicheSize:           Int,
    niche:               Seq[ProfileElement],
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition])

  def apply[P](
    niche:      Seq[ProfileElement],
    genome:     Genome,
    objective:  Objectives,
    nicheSize:  Int,
    outputs:    Seq[Val[?]]                  = Seq(),
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective, stochastic.option) match {
      case None =>
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        def validation: Validate =
          Objectives.validate(objective, outputs) ++
            indexesOfProfiledTry(niche, genome).collect:
              case util.Failure(t) => t

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicProfile(
            genome = genome,
            objectives = exactObjectives,
            phenotypeContent = phenotypeContent,
            niche = niche,
            operatorExploration = EvolutionWorkflow.operatorExploration,
            nicheSize = nicheSize,
            reject = reject.option),
          genome,
          phenotypeContent,
          validate = validation
        )
      case Some(stochasticValue) =>
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs) ++
            indexesOfProfiledTry(niche, genome).collect:
              case util.Failure(t) => t

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticProfile(
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

  given EvolutionMethod[ProfileEvolution] =
    p =>
      def profile =
        p.profile match
          case p: Profile.ProfileElement => Seq(p)
          case p: Seq[Profile.ProfileElement] => p

      Profile(
        niche = profile,
        genome = p.genome,
        objective = p.objective,
        outputs = p.evaluation.outputs,
        stochastic = p.stochastic,
        nicheSize = p.nicheSize,
        reject = p.reject
      )

  given ExplorationMethod[ProfileEvolution, EvolutionWorkflow] =
    p =>
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(p.genome),
        scope = p.scope
      )

  given ExplorationMethodSetter[ProfileEvolution, EvolutionPattern] = (e, p) => e.copy(distribution = p)

}

import monocle.macros._

case class ProfileEvolution(
  profile:      Profile.ProfileElements,
  genome:       Genome,
  objective:    Objectives,
  evaluation:   DSL,
  termination:  OMTermination,
  nicheSize:    Int                          = 10,
  stochastic:   OptionalArgument[Stochastic] = None,
  reject:       OptionalArgument[Condition]  = None,
  parallelism:  Int                          = EvolutionWorkflow.parallelism,
  distribution: EvolutionPattern             = SteadyState(),
  suggestion:   Suggestion                   = Suggestion.empty,
  scope:        DefinitionScope              = "profile")