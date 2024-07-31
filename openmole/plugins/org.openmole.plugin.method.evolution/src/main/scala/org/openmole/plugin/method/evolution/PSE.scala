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
import org.openmole.core.argument.{FromContext, OptionalArgument}
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
import org.openmole.core.setter.{ DefinitionScope, ValueAssignment }
import org.openmole.plugin.method.evolution.Genome.{ GenomeBound, Suggestion }
import org.openmole.plugin.method.evolution.Objective._
import org.openmole.tool.types.ToDouble
import squants.time.Time

import scala.language.higherKinds
import scala.reflect.ClassTag

import monocle._
import monocle.syntax.all._

object PSE {

  case class DeterministicPSE(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition],
    grid:                Seq[PatternAxe]
  )

  object DeterministicPSE {

    import mgo.evolution.algorithm.{ CDGenome, PSE ⇒ MGOPSE, _ }
    import cats.data._

    given MGOAPI.Integration[DeterministicPSE, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[DeterministicPSE, (Vector[Double], Vector[Int]), Phenotype] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicPSE) = new Ops {
        override def metadata(state: S, saveOption: SaveOption): EvolutionMetadata =
          EvolutionMetadata.PSE(
            genome = MetadataGeneration.genomeData(om.genome),
            objective = om.objectives.map(MetadataGeneration.objectiveData),
            grid = MetadataGeneration.grid(om.grid),
            generation = generationLens.get(state),
            saveOption = saveOption
          )

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)

        def buildGenome(vs: Vector[Variable[?]]): G = buildGenome(Genome.fromVariables(vs, om.genome))
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = EvolutionState[HitMapState](s = Map())

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val toFitness = Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context)
          val res = MGOPSE.result[Phenotype](population, Genome.continuous(om.genome), toFitness andThen om.pattern)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype).map(toFitness))
          val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

          val outputValues =
            if includeOutputs
            then DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype))
            else Seq()

          genomes ++ fitness ++ Seq(generated) ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGOPSE.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        private def pattern(phenotype: Phenotype) = FromContext { p ⇒
          import p._
          om.pattern(Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context).apply(phenotype))
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          MGOPSE.adaptiveBreeding[Phenotype](
            n,
            om.operatorExploration,
            discrete,
            pattern(_).from(context),
            rejectValue)(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            MGOPSE.elitism[Phenotype](pattern(_).from(context), Genome.continuous(om.genome)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state: S)
        def migrateFromIsland(population: Vector[I], state: S, generation: Long) = DeterministicGAIntegration.migrateFromIsland(population, generation)
      }

    }
  }

  case class StochasticPSE(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double,
    reject:              Option[Condition],
    grid:                Seq[PatternAxe])

  object StochasticPSE {

    import mgo.evolution.algorithm.{ CDGenome, NoisyPSE ⇒ MGONoisyPSE, _ }
    import cats.data._

    given MGOAPI.Integration[StochasticPSE, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[StochasticPSE, (Vector[Double], Vector[Int]), Phenotype] { api ⇒
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticPSE) = new Ops:
        override def metadata(state: S, saveOption: SaveOption) =
          EvolutionMetadata.StochasticPSE(
            genome = MetadataGeneration.genomeData(om.genome),
            objective = om.objectives.map(MetadataGeneration.objectiveData),
            sample = om.historySize,
            grid = MetadataGeneration.grid(om.grid),
            generation = generationLens.get(state),
            saveOption = saveOption
          )

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)
        def buildGenome(v: (Vector[Double], Vector[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
        def buildGenome(vs: Vector[Variable[?]]) = buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = MGONoisyPSE.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[HitMapState](s = Map())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val aggregate = Objective.aggregate(om.phenotypeContent, om.objectives).from(context)
          val res = MGONoisyPSE.result(population, aggregate, om.pattern, Genome.continuous(om.genome))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(r => aggregate(r.individual.phenotypeHistory.toVector)))
          val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
          val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

          val outputValues =
            if includeOutputs
            then StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory))
            else Seq()

          genomes ++ fitness ++ Seq(samples, generated) ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
            MGONoisyPSE.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
            MGONoisyPSE.adaptiveBreeding[Phenotype](
              n,
              om.operatorExploration,
              om.cloneProbability,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              discrete,
              om.pattern,
              rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._

            MGONoisyPSE.elitism[Phenotype](
              om.pattern,
              Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
              om.historySize,
              Genome.continuous(om.genome)) apply (s, population, candidates, rng)


        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], state: S, generation: Long) = StochasticGAIntegration.migrateFromIsland(population, generation)

    }
  }

  object PatternAxe {
    implicit def fromInExactToPatternAxe[T, D](v: In[T, D])(implicit fix: FixDomain[D, Double], te: ToObjective[T]): PatternAxe = PatternAxe(te.apply(v.value), fix(v.domain).domain.toVector)
    //    implicit def fromInNoisyToPatternAxe[T, D](v: In[T, D])(implicit fix: FixDomain[D, Double], te: ToNoisyObjective[T]) = PatternAxe(te.apply(v.value), fix(v.domain).domain.toVector)

    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: FixDomain[D, Double]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).domain.toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: FixDomain[D, Int]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).domain.toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: FixDomain[D, Long]): PatternAxe =
      PatternAxe(f.value, fix(f.domain).domain.toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective, scale: Vector[Double])

  def apply(
    genome:     Genome,
    objective:  Seq[PatternAxe],
    outputs:    Seq[Val[?]]                  = Seq(),
    stochastic: OptionalArgument[Stochastic] = None,
    reject:     OptionalArgument[Condition]  = None
  ) =
    EvolutionWorkflow.stochasticity(objective.map(_.p), stochastic.option) match {
      case None ⇒
        val exactObjectives = Objectives.toExact(objective.map(_.p))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicPSE(
            mgo.evolution.niche.irregularGrid(objective.map(_.scale).toVector),
            genome,
            phenotypeContent,
            exactObjectives,
            EvolutionWorkflow.operatorExploration,
            reject = reject.option,
            grid = objective),
          genome,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective.map(_.p))
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate = {
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)
        }

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticPSE(
            pattern = mgo.evolution.niche.irregularGrid(objective.map(_.scale).toVector),
            genome = genome,
            phenotypeContent = phenotypeContent,
            objectives = noisyObjectives,
            historySize = stochasticValue.sample,
            cloneProbability = stochasticValue.reevaluate,
            operatorExploration = EvolutionWorkflow.operatorExploration,
            reject = reject.option,
            grid = objective),
          genome,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )
    }

}

import monocle.macros._
import EvolutionWorkflow._

object PSEEvolution {

  import org.openmole.core.dsl.DSL

  given EvolutionMethod[PSEEvolution] =
    p =>
      PSE(
        genome = p.genome,
        objective = p.objective,
        outputs = p.evaluation.outputs,
        stochastic = p.stochastic,
        reject = p.reject
      )

  given ExplorationMethod[PSEEvolution, EvolutionWorkflow] =
    p ⇒
      EvolutionWorkflow(
        method = p,
        evaluation = p.evaluation,
        termination = p.termination,
        parallelism = p.parallelism,
        distribution = p.distribution,
        suggestion = p.suggestion(p.genome),
        scope = p.scope
      )

  given ExplorationMethodSetter[PSEEvolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)

}

case class PSEEvolution(
  genome:       Genome,
  objective:    Seq[PSE.PatternAxe],
  evaluation:   DSL,
  termination:  OMTermination,
  stochastic:   OptionalArgument[Stochastic] = None,
  reject:       OptionalArgument[Condition]  = None,
  parallelism:  Int                          = EvolutionWorkflow.parallelism,
  distribution: EvolutionPattern             = SteadyState(),
  suggestion:   Suggestion                   = Suggestion.empty,
  scope:        DefinitionScope              = "pse")