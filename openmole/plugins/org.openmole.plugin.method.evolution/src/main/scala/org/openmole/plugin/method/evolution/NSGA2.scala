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

import cats.implicits._
import monocle.macros.{ GenLens }
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.method.evolution.Genome.Suggestion
import squants.time.Time

import monocle._
import monocle.syntax.all._

object NSGA2 {

  object DeterministicNSGA2 {
    import mgo.evolution.algorithm.{ CDGenome, NSGA2 ⇒ MGONSGA2, _ }
    
    given MGOAPI.Integration[DeterministicNSGA2, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[DeterministicNSGA2, (IArray[Double], IArray[Int]), Phenotype]:
      type G = CDGenome.Genome
      type I = CDGenome.DeterministicIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: DeterministicNSGA2) = new Ops:
        def startTimeLens = Focus[S](_.startTime)
        def generationLens = Focus[S](_.generation)
        def evaluatedLens = Focus[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)

        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] = 
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.DeterministicIndividual.buildIndividual(genome, phenotype, state.generation, false)

        def initialState = EvolutionState[Unit](s = ())

        override def metadata(state: S, saveOption: SaveOption) =
          EvolutionMetadata.NSGA2(
            genome = MetadataGeneration.genomeData(om.genome),
            objective = om.objectives.map(MetadataGeneration.objectiveData),
            populationSize = om.mu,
            generation = generationLens.get(state),
            saveOption = saveOption
          )

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p._
            val res = MGONSGA2.result[Phenotype](population, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputsValues = if (includeOutputs) DeterministicGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotype)) else Seq()

            genomes ++ fitness ++ Seq(generated) ++ outputsValues


        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGONSGA2.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGONSGA2.adaptiveBreeding[S, Phenotype](n, om.operatorExploration, discrete, Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context), rejectValue)(s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            MGONSGA2.elitism[S, Phenotype](om.mu, Genome.continuous(om.genome), Objective.toFitnessFunction(om.phenotypeContent, om.objectives).from(context))(s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (DeterministicGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (DeterministicGAIntegration.migrateFromIsland(population, initialState.generation), state)


  }

  case class DeterministicNSGA2(
    mu:                  Int,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    operatorExploration: Double,
    reject:              Option[Condition])

  object StochasticNSGA2 {
    import mgo.evolution.algorithm.{ CDGenome, NoisyNSGA2 ⇒ MGONoisyNSGA2, _ }

    given MGOAPI.Integration[StochasticNSGA2, (IArray[Double], IArray[Int]), Phenotype] = new MGOAPI.Integration[StochasticNSGA2, (IArray[Double], IArray[Int]), Phenotype] {
      type G = CDGenome.Genome
      type I = CDGenome.NoisyIndividual.Individual[Phenotype]
      type S = EvolutionState[Unit]

      def iManifest = implicitly[Manifest[I]]
      def gManifest = implicitly
      def sManifest = implicitly

      def operations(om: StochasticNSGA2) = new Ops:

        override def metadata(state: S, saveOption: SaveOption) =
          EvolutionMetadata.StochasticNSGA2(
            genome = MetadataGeneration.genomeData(om.genome),
            objective = om.objectives.map(MetadataGeneration.objectiveData),
            sample = om.historySize,
            populationSize = om.mu,
            generation = generationLens.get(state),
            saveOption = saveOption
          )

        def startTimeLens = GenLens[S](_.startTime)
        def generationLens = GenLens[S](_.generation)
        def evaluatedLens = GenLens[S](_.evaluated)

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get, CDGenome.discreteValues.get)(genome)
        def buildGenome(vs: Vector[Variable[?]]) =
          def buildGenome(v: (IArray[Double], IArray[Int])): G = CDGenome.buildGenome(v._1, None, v._2, None)
          buildGenome(Genome.fromVariables(vs, om.genome))

        def genomeToVariables(g: G): FromContext[Vector[Variable[?]]] =
          val (cs, is) = genomeValues(g)
          Genome.toVariables(om.genome, cs, is, scale = true)

        def buildIndividual(genome: G, phenotype: Phenotype, state: S) = CDGenome.NoisyIndividual.buildIndividual(genome, phenotype, state.generation, false)
        def initialState = EvolutionState[Unit](s = ())

        def aggregate = Objective.aggregate(om.phenotypeContent, om.objectives)

        def result(population: Vector[I], state: S, keepAll: Boolean, includeOutputs: Boolean) =
          FromContext: p ⇒
            import p._

            val res = MGONoisyNSGA2.result(population, aggregate.from(context), Genome.continuous(om.genome), keepAll = keepAll)
            val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false)
            val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.fitness))
            val samples = Variable(GAIntegration.samplesVal.array, res.map(_.replications).toArray)
            val generated = Variable(GAIntegration.generatedVal.array, res.map(_.individual.generation).toArray)

            val outputValues =
              if includeOutputs
              then StochasticGAIntegration.outputValues(om.phenotypeContent, res.map(_.individual.phenotypeHistory))
              else Seq()

            genomes ++ fitness ++ Seq(samples, generated) ++ outputValues

        def initialGenomes(n: Int, rng: scala.util.Random) =
          FromContext: p ⇒
            import p._
            val continuous = Genome.continuous(om.genome)
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGONoisyNSGA2.initialGenomes(n, continuous, discrete, rejectValue, rng)

        def breeding(individuals: Vector[I], n: Int, s: S, rng: util.Random) =
          FromContext: p ⇒
            import p._
            val discrete = Genome.discrete(om.genome)
            val rejectValue = om.reject.map(f ⇒ GAIntegration.rejectValue[G](f, om.genome, _.continuousValues, _.discreteValues).from(context))
            MGONoisyNSGA2.adaptiveBreeding[S, Phenotype](n, om.operatorExploration, om.cloneProbability, aggregate.from(context), discrete, rejectValue) apply (s, individuals, rng)

        def elitism(population: Vector[I], candidates: Vector[I], s: S, rng: util.Random) =
          FromContext: p ⇒
            import p._
            MGONoisyNSGA2.elitism[S, Phenotype](om.mu, om.historySize, aggregate.from(context), Genome.continuous(om.genome)) apply (s, population, candidates, rng)

        def mergeIslandState(state: S, islandState: S): S = state
        def migrateToIsland(population: Vector[I], state: S) = (StochasticGAIntegration.migrateToIsland(population), state)
        def migrateFromIsland(population: Vector[I], initialState: S, state: S) = (StochasticGAIntegration.migrateFromIsland(population, initialState.generation), state)

    }
  }

  case class StochasticNSGA2(
    mu:                  Int,
    operatorExploration: Double,
    genome:              Genome,
    phenotypeContent:    PhenotypeContent,
    objectives:          Seq[Objective],
    historySize:         Int,
    cloneProbability:    Double,
    reject:              Option[Condition]
  )

  def apply(
    genome:         Genome,
    objective:      Objectives,
    outputs:        Seq[Val[?]]                  = Seq(),
    populationSize: Int                          = 200,
    stochastic:     OptionalArgument[Stochastic] = None,
    reject:         OptionalArgument[Condition]  = None
  ): EvolutionWorkflow =
    EvolutionWorkflow.stochasticity(objective, stochastic.option) match
      case None ⇒
        val exactObjectives = Objectives.toExact(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(exactObjectives), outputs)

        EvolutionWorkflow.deterministicGAIntegration(
          DeterministicNSGA2(populationSize, genome, phenotypeContent, exactObjectives, EvolutionWorkflow.operatorExploration, reject),
          genome,
          phenotypeContent,
          validate = Objectives.validate(exactObjectives, outputs)
        )
      case Some(stochasticValue) ⇒
        val noisyObjectives = Objectives.toNoisy(objective)
        val phenotypeContent = PhenotypeContent(Objectives.prototypes(noisyObjectives), outputs)

        def validation: Validate =
          val aOutputs = outputs.map(_.toArray)
          Objectives.validate(noisyObjectives, aOutputs)

        EvolutionWorkflow.stochasticGAIntegration(
          StochasticNSGA2(populationSize, EvolutionWorkflow.operatorExploration, genome, phenotypeContent, noisyObjectives, stochasticValue.sample, stochasticValue.reevaluate, reject.option),
          genome,
          phenotypeContent,
          stochasticValue,
          validate = validation
        )

}

import EvolutionWorkflow.*

object NSGA2Evolution:

  import org.openmole.core.dsl.DSL

  given EvolutionMethod[NSGA2Evolution] =
    p =>
      NSGA2(
        populationSize = p.populationSize,
        genome = p.genome,
        objective = p.objective,
        outputs = p.evaluation.outputs,
        stochastic = p.stochastic,
        reject = p.reject
      )

  given ExplorationMethod[NSGA2Evolution, EvolutionWorkflow] =
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

  given ExplorationMethodSetter[NSGA2Evolution, EvolutionPattern] = (e, p) ⇒ e.copy(distribution = p)


case class NSGA2Evolution(
  genome:         Genome,
  objective:      Objectives,
  evaluation:     DSL,
  termination:    OMTermination,
  populationSize: Int                          = 200,
  stochastic:     OptionalArgument[Stochastic] = None,
  reject:         OptionalArgument[Condition]  = None,
  parallelism:    Int                          = EvolutionWorkflow.parallelism,
  distribution:   EvolutionPattern             = SteadyState(),
  suggestion:     Suggestion                   = Suggestion.empty,
  scope:          DefinitionScope              = "nsga2")
