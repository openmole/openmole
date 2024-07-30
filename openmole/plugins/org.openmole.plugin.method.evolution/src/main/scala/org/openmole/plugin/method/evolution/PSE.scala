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

object PSEAlgorithm {

  import mgo.tools._
  import monocle.macros.{ GenLens }
  
  import scala.language.higherKinds
  import cats.implicits._
  import mgo.evolution.algorithm._

  import GenomeVectorDouble._
  import CDGenome._

  case class Individual[P](
    genome:        CDGenome.Genome,
    phenotype:     P,
    foundedIsland: Boolean         = false)

  case class Result[P](continuous: Vector[Double], discrete: Vector[Int], pattern: Vector[Int], phenotype: Vector[Double], individual: Individual[P])

  def result[P](population: Vector[Individual[P]], continuous: Vector[C], pattern: Vector[Double] ⇒ Vector[Int], phenotype: P ⇒ Vector[Double]) =
    population.map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Focus[Individual[P]](_.genome) andThen discreteValues get i,
        pattern(phenotype(i.phenotype)),
        phenotype(i.phenotype),
        i
      )
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
      Focus[Individual[P]](_.genome).get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      i => pattern(i.phenotype),
      buildGenome,
      lambda,
      reject,
      operatorExploration,
      hitmap)

  def elitism[S, P: CanBeNaN](pattern: P ⇒ Vector[Int], continuous: Vector[C], hitmap: monocle.Lens[S, HitMap]) =
    PSEOperations.elitism[S, Individual[P], P](
      i ⇒ values(i.genome, continuous),
      i => i.phenotype,
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

  case class Individual[P](
    genome:           CDGenome.Genome,
    historyAge:       Long,
    phenotypeHistory: Array[P])

  def buildIndividual[P: Manifest](genome: CDGenome.Genome, phenotype: P) = Individual(genome, 1, Array(phenotype))
  def vectorPhenotype[P: Manifest] = Focus[Individual[P]](_.phenotypeHistory) andThen arrayToVectorIso

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
      Focus[Individual[P]](_.genome).get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      vectorPhenotype.get andThen aggregation andThen pattern,
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
      i ⇒ values(i.genome, continuous),
      vectorPhenotype[P],
      aggregation,
      pattern,
      Focus[Individual[P]](_.historyAge),
      historySize,
      hitmap)

  def expression[P: Manifest](fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ P, continuous: Vector[C]): (util.Random, Genome) ⇒ Individual[P] =
    noisy.expression[CDGenome.Genome, Individual[P], P](
      values(_, continuous),
      buildIndividual)(fitness)

  def aggregate[P: Manifest](i: Individual[P], aggregation: Vector[P] ⇒ Vector[Double], pattern: Vector[Double] ⇒ Vector[Int], continuous: Vector[C]) =
    (
      scaleContinuousValues(continuousValues.get(i.genome), continuous),
      i.focus(_.genome) andThen discreteValues get,
      aggregation(vectorPhenotype.get(i)),
      (vectorPhenotype.get andThen aggregation andThen pattern)(i),
      i.phenotypeHistory.size)

  case class Result[P](continuous: Vector[Double], discrete: Vector[Int], phenotype: Vector[Double], pattern: Vector[Int], replications: Int, individual: Individual[P])

  def result[P: Manifest](
    population:  Vector[Individual[P]],
    aggregation: Vector[P] ⇒ Vector[Double],
    pattern:     Vector[Double] ⇒ Vector[Int],
    continuous:  Vector[C]) =
    population.map {
      i ⇒
        val (c, d, f, p, r) = aggregate(i, aggregation, pattern, continuous)
        Result(c, d, f, p, r, i)
    }

}

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

    import mgo.evolution.algorithm.{ PSE ⇒ _, _ }
    import cats.data._

    given MGOAPI.Integration[DeterministicPSE, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[DeterministicPSE, (Vector[Double], Vector[Int]), Phenotype] { api ⇒
      type G = CDGenome.Genome
      type I = PSEAlgorithm.Individual[Phenotype]
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

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context, state: S) = PSEAlgorithm.buildIndividual(genome, phenotype)

        def initialState = EvolutionState[HitMapState](s = Map())

        def afterEvaluated(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterEvaluated[S, I](g, Focus[S](_.evaluated))(s, population)
        def afterGeneration(g: Long, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterGeneration[S, I](g, Focus[S](_.generation))(s, population)
        def afterDuration(d: Time, s: S, population: Vector[I]): Boolean = mgo.evolution.stop.afterDuration[S, I](d, Focus[S](_.startTime))(s, population)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._
          val res = PSEAlgorithm.result[Phenotype](population, Genome.continuous(om.genome), om.pattern, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype))

          val outputValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

          genomes ++ fitness ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          PSEAlgorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        private def pattern(phenotype: Phenotype) = FromContext { p ⇒
          import p._
          om.pattern(Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context).apply(phenotype))
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          PSEAlgorithm.adaptiveBreeding[S, Phenotype](
            n,
            rejectValue,
            om.operatorExploration,
            discrete,
            pattern(_).from(context),
            Focus[S](_.s))(s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val (s2, elited) = PSEAlgorithm.elitism[S, Phenotype](pattern(_).from(context), Genome.continuous(om.genome), Focus[S](_.s)) apply (s, population, candidates, rng)
          val s3 = DeterministicGAIntegration.updateState(s2, generationLens, evaluatedLens, evaluated)
          (s3, elited)
        }

        def migrateToIsland(population: Vector[I]) = population.map(Focus[I](_.foundedIsland).set(true))

        def migrateFromIsland(population: Vector[I], state: S) =
          population.filter(i ⇒ !i.foundedIsland).
            map(Focus[I](_.foundedIsland).set(false))
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

    import mgo.evolution.algorithm.{ PSE ⇒ _, NoisyPSE ⇒ _, _ }
    import cats.data._

    given MGOAPI.Integration[StochasticPSE, (Vector[Double], Vector[Int]), Phenotype] = new MGOAPI.Integration[StochasticPSE, (Vector[Double], Vector[Int]), Phenotype] { api ⇒
      type G = CDGenome.Genome
      type I = NoisyPSEAlgorithm.Individual[Phenotype]
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticPSE) = new Ops {
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

        def buildIndividual(genome: G, phenotype: Phenotype, context: Context, state: S) = NoisyPSEAlgorithm.buildIndividual(genome, phenotype)
        def initialState = EvolutionState[HitMapState](s = Map())

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) = FromContext { p ⇒
          import p._

          val res = NoisyPSEAlgorithm.result(population, Objective.aggregate(om.phenotypeContent, om.objectives).from(context), om.pattern, Genome.continuous(om.genome))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype))
          val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)

          val outputValues = if (includeOutputs) StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory)) else Seq()

          genomes ++ fitness ++ Seq(samples) ++ outputValues
        }

        def initialGenomes(n: Int, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val continuous = Genome.continuous(om.genome)
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyPSEAlgorithm.initialGenomes(n, continuous, discrete, rejectValue, rng)
        }

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) = FromContext { p ⇒
          import p._
          val discrete = Genome.discrete(om.genome)
          val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues.toVector, _.discreteValues.toVector).from(context))
          NoisyPSEAlgorithm.adaptiveBreeding[S, Phenotype](
            n,
            rejectValue,
            om.operatorExploration,
            om.cloneProbability,
            Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
            discrete,
            om.pattern,
            Focus[S](_.s)) apply (s, individuals, rng)
        }

        def elitism(population: Vector[I], candidates: Vector[I], s: S, evaluated: Long, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._

            val (s2, elited) =
              NoisyPSEAlgorithm.elitism[S, Phenotype](
                om.pattern,
                Objective.aggregate(om.phenotypeContent, om.objectives).from(context),
                om.historySize,
                Genome.continuous(om.genome),
                Focus[S](_.s)) apply (s, population, candidates, rng)

            val s3 = StochasticGAIntegration.updateState(s2, generationLens, evaluatedLens, evaluated)
            (s3, elited)

        def migrateToIsland(population: Vector[I]) =
          StochasticGAIntegration.migrateToIsland[I](population, Focus[I](_.historyAge))

        def migrateFromIsland(population: Vector[I], state: S) =
          StochasticGAIntegration.migrateFromIsland[I, Phenotype](population, Focus[I](_.historyAge), Focus[I](_.phenotypeHistory))

      }

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