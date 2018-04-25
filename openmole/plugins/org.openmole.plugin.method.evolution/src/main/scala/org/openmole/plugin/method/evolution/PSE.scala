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

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.tool.random._
import monocle.macros._
import cats.implicits._
import org.openmole.core.expansion.FromContext

object PSEAlgorithm {

  import mgo._
  import mgo.breeding._
  import mgo.contexts._
  import mgo.ranking._
  import mgo.elitism._
  import mgo.tools._
  import mgo.niche._

  import monocle.macros.{ GenLens, Lenses }

  import scala.language.higherKinds
  import cats.implicits._
  import freedsl.io.IOInterpreter
  import freedsl.random.RandomInterpreter
  import freedsl.system.SystemInterpreter
  import mgo.algorithm._

  import GenomeVectorDouble._
  import CDGenome._

  @Lenses case class Individual(
    genome:        Genome,
    phenotype:     Array[Double],
    mapped:        Boolean       = false,
    foundedIsland: Boolean       = false)

  case class Result(continuous: Vector[Double], discrete: Vector[Int], pattern: Vector[Int], phenotype: Vector[Double])

  def result(population: Vector[Individual], continuous: Vector[C], pattern: Vector[Double] ⇒ Vector[Int]) =
    population.map { i ⇒
      Result(
        scaleContinuousValues(continuousValues.get(i.genome), continuous),
        Individual.genome composeLens discreteValues get i,
        pattern(i.phenotype.toVector),
        i.phenotype.toVector)
    }

  def state[M[_]: cats.Monad: StartTime: Random: Generation](implicit hitmap: HitMap[M]) = for {
    map ← hitmap.get
    s ← mgo.algorithm.state[M, Map[Vector[Int], Int]](map)
  } yield s

  def buildIndividual(g: Genome, f: Vector[Double]) = Individual(g, f.toArray)
  def vectorPhenotype = Individual.phenotype composeLens arrayToVectorLens

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def adaptiveBreeding[M[_]: Generation: Random: cats.Monad: HitMap](
    lambda:              Int,
    operatorExploration: Double,
    discrete:            Vector[D],
    pattern:             Vector[Double] ⇒ Vector[Int]): Breeding[M, Individual, Genome] =
    PSEOperations.adaptiveBreeding[M, Individual, Genome](
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      vectorPhenotype.get _ andThen pattern,
      buildGenome,
      lambda,
      operatorExploration)

  def elitism[M[_]: cats.Monad: StartTime: Random: HitMap: Generation](pattern: Vector[Double] ⇒ Vector[Int], continuous: Vector[C]) =
    PSEOperations.elitism[M, Individual, Vector[Double]](
      i ⇒ values(Individual.genome.get(i), continuous),
      vectorPhenotype.get,
      pattern,
      Individual.mapped)

  def expression(phenotype: (Vector[Double], Vector[Int]) ⇒ Vector[Double], continuous: Vector[C]): Genome ⇒ Individual =
    deterministic.expression[Genome, Individual](
      values(_, continuous),
      buildIndividual,
      phenotype)

  object PSEImplicits {
    def apply(state: EvolutionState[Map[Vector[Int], Int]]): PSEImplicits =
      PSEImplicits()(GenerationInterpreter(state.generation), RandomInterpreter(state.random), StartTimeInterpreter(state.startTime), IOInterpreter(), HitMapInterpreter(state.s), SystemInterpreter())
  }

  case class PSEImplicits(implicit generationInterpreter: GenerationInterpreter, randomInterpreter: RandomInterpreter, startTimeInterpreter: StartTimeInterpreter, iOInterpreter: IOInterpreter, hitMapInterpreter: HitMapInterpreter, systemInterpreter: SystemInterpreter)

  def run[T](rng: util.Random)(f: PSEImplicits ⇒ T): T = {
    val state = EvolutionState[Map[Vector[Int], Int]](random = rng, s = Map.empty)
    run(state)(f)
  }

  def run[T, S](state: EvolutionState[HitMapState])(f: PSEImplicits ⇒ T): T = f(PSEImplicits(state))

}

object NoisyPSEAlgorithm {

  import mgo._
  import mgo.breeding._
  import mgo.contexts._
  import monocle.macros._
  import mgo.algorithm._
  import mgo.algorithm.CDGenome._

  @Lenses case class Individual(
    genome:           Genome,
    historyAge:       Long,
    phenotypeHistory: Array[Array[Double]],
    mapped:           Boolean              = false,
    foundedIsland:    Boolean              = false)

  def buildIndividual(genome: Genome, phenotype: Vector[Double]) = Individual(genome, 1, Array(phenotype.toArray))
  def vectorPhenotype = Individual.phenotypeHistory composeLens array2ToVectorLens

  def state[M[_]: cats.Monad: StartTime: Random: Generation: HitMap] = PSEAlgorithm.state[M]

  def initialGenomes[M[_]: cats.Monad: Random](lambda: Int, continuous: Vector[C], discrete: Vector[D]) =
    CDGenome.initialGenomes[M](lambda, continuous, discrete)

  def adaptiveBreeding[M[_]: cats.Monad: Random: Generation: HitMap](
    lambda:              Int,
    operatorExploration: Double,
    cloneProbability:    Double,
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double],
    discrete:            Vector[D],
    pattern:             Vector[Double] ⇒ Vector[Int]): Breeding[M, Individual, Genome] =
    NoisyPSEOperations.adaptiveBreeding[M, Individual, Genome](
      Individual.genome.get,
      continuousValues.get,
      continuousOperator.get,
      discreteValues.get,
      discreteOperator.get,
      discrete,
      vectorPhenotype.get _ andThen aggregation andThen pattern,
      buildGenome,
      lambda,
      operatorExploration,
      cloneProbability)

  def elitism[M[_]: cats.Monad: Random: HitMap: Generation](
    pattern:     Vector[Double] ⇒ Vector[Int],
    aggregation: Vector[Vector[Double]] ⇒ Vector[Double],
    historySize: Int,
    continuous:  Vector[C]) =
    NoisyPSEOperations.elitism[M, Individual, Vector[Double]](
      i ⇒ values(Individual.genome.get(i), continuous),
      vectorPhenotype,
      aggregation,
      pattern,
      Individual.mapped,
      Individual.historyAge,
      historySize)

  def expression(fitness: (util.Random, Vector[Double], Vector[Int]) ⇒ Vector[Double], continuous: Vector[C]): (util.Random, Genome) ⇒ Individual =
    noisy.expression[Genome, Individual](
      values(_, continuous),
      buildIndividual)(fitness)

  def aggregate(i: Individual, aggregation: Vector[Vector[Double]] ⇒ Vector[Double], pattern: Vector[Double] ⇒ Vector[Int], continuous: Vector[C]) =
    (
      scaleContinuousValues(continuousValues.get(i.genome), continuous),
      Individual.genome composeLens discreteValues get i,
      aggregation(vectorPhenotype.get(i)),
      (vectorPhenotype.get _ andThen aggregation andThen pattern)(i),
      Individual.phenotypeHistory.get(i).size)

  case class Result(continuous: Vector[Double], discrete: Vector[Int], phenotype: Vector[Double], pattern: Vector[Int], replications: Int)

  def result(
    population:  Vector[Individual],
    aggregation: Vector[Vector[Double]] ⇒ Vector[Double],
    pattern:     Vector[Double] ⇒ Vector[Int],
    continuous:  Vector[C]) =
    population.map {
      i ⇒
        val (c, d, f, p, r) = aggregate(i, aggregation, pattern, continuous)
        Result(c, d, f, p, r)
    }

  def run[T](rng: util.Random)(f: PSEAlgorithm.PSEImplicits ⇒ T): T = PSEAlgorithm.run(rng)(f)
  def run[T](state: EvolutionState[Map[Vector[Int], Int]])(f: PSEAlgorithm.PSEImplicits ⇒ T): T = PSEAlgorithm.run(state)(f)

}

object PSE {

  case class DeterministicParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genome:              Genome,
    objectives:          Objectives,
    operatorExploration: Double
  )

  object DeterministicParams {

    import mgo.algorithm.{ PSE ⇒ _, _ }
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = PSEAlgorithm.Individual
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: PSEAlgorithm.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        PSEAlgorithm.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: HitMap, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← PSEAlgorithm.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: DeterministicParams) = new Ops {

        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation

        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)
        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = PSEAlgorithm.buildIndividual(genome, phenotype)

        def initialState(rng: util.Random) = EvolutionState[HitMapState](random = rng, s = Map())

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._

          val res = PSEAlgorithm.result(population, Genome.continuous(om.genome).from(context), om.pattern)
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)

          genomes ++ fitness
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              implicitly[Generation[DSL]]

              zipWithState(
                PSEAlgorithm.initialGenomes[DSL](n, continuous, discrete)
              ).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(
                PSEAlgorithm.adaptiveBreeding[DSL](
                  n,
                  om.operatorExploration,
                  discrete,
                  om.pattern).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← PSEAlgorithm.elitism[DSL](om.pattern, continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def migrateToIsland(population: Vector[I]) =
          population.map(PSEAlgorithm.Individual.foundedIsland.set(true))

        def migrateFromIsland(population: Vector[I], state: S) =
          population.filter(i ⇒ !PSEAlgorithm.Individual.foundedIsland.get(i)).
            map(PSEAlgorithm.Individual.mapped.set(false)).
            map(PSEAlgorithm.Individual.foundedIsland.set(false))
      }

    }
  }

  case class StochasticParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double],
    genome:              Genome,
    objectives:          Objectives,
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double
  )

  object StochasticParams {

    import mgo.algorithm.{ PSE ⇒ _, NoisyPSE ⇒ _, _ }
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, (Vector[Double], Vector[Int]), Vector[Double]] { api ⇒
      type G = CDGenome.Genome
      type I = NoisyPSEAlgorithm.Individual
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: PSEAlgorithm.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        NoisyPSEAlgorithm.run(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: HitMap, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← NoisyPSEAlgorithm.state[M]
        } yield (newState, t)
      }

      def afterGeneration(g: Long, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterGeneration[DSL, I](g).run(population)).eval
      }

      def afterDuration(d: squants.Time, population: Vector[I]): M[Boolean] = interpret { impl ⇒
        import impl._
        zipWithState(mgo.afterDuration[DSL, I](d).run(population)).eval
      }

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)

        def generation(s: S) = s.generation
        def genomeValues(genome: G) = MGOAPI.paired(CDGenome.continuousValues.get _, CDGenome.discreteValues.get _)(genome)

        def buildIndividual(genome: G, phenotype: Vector[Double], context: Context) = NoisyPSEAlgorithm.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[HitMapState](random = rng, s = Map())

        def result(population: Vector[I], state: S) = FromContext { p ⇒
          import p._
          import org.openmole.core.context._

          val res = NoisyPSEAlgorithm.result(population, om.aggregation, om.pattern, Genome.continuous(om.genome).from(context))
          val genomes = GAIntegration.genomesOfPopulationToVariables(om.genome, res.map(_.continuous) zip res.map(_.discrete), scale = false).from(context)
          val fitness = GAIntegration.objectivesOfPopulationToVariables(om.objectives, res.map(_.phenotype)).from(context)
          val samples = Variable(GAIntegration.samples.array, res.map(_.replications).toArray)

          genomes ++ fitness ++ Seq(samples)
        }

        def initialGenomes(n: Int) =
          (Genome.continuous(om.genome) map2 Genome.discrete(om.genome)) { (continuous, discrete) ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyPSEAlgorithm.initialGenomes[DSL](n, continuous, discrete)).eval
            }
          }

        def breeding(individuals: Vector[I], n: Int) =
          Genome.discrete(om.genome).map { discrete ⇒
            interpret { impl ⇒
              import impl._
              zipWithState(NoisyPSEAlgorithm.adaptiveBreeding[DSL](
                n,
                om.operatorExploration,
                om.cloneProbability,
                om.aggregation,
                discrete,
                om.pattern).run(individuals)).eval
            }
          }

        def elitism(individuals: Vector[I]) =
          Genome.continuous(om.genome).map { continuous ⇒
            interpret { impl ⇒
              import impl._
              def step =
                for {
                  elited ← NoisyPSEAlgorithm.elitism[DSL](
                    om.pattern,
                    om.aggregation,
                    om.historySize,
                    continuous) apply individuals
                  _ ← mgo.elitism.incrementGeneration[DSL]
                } yield elited

              zipWithState(step).eval
            }
          }

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def migrateToIsland(population: Vector[I]) =
          population.map(NoisyPSEAlgorithm.Individual.foundedIsland.set(true)).map(NoisyPSEAlgorithm.Individual.historyAge.set(0))

        def migrateFromIsland(population: Vector[I], state: S) =
          population.filter(i ⇒ !NoisyPSEAlgorithm.Individual.foundedIsland.get(i)).
            map(NoisyPSEAlgorithm.Individual.mapped.set(false)).
            map(NoisyPSEAlgorithm.Individual.foundedIsland.set(false))

      }

    }
  }

  import org.openmole.core.dsl._

  object PatternAxe {

    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: Fix[D, Long]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective, scale: Vector[Double])

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe],
    stochastic: OptionalArgument[Stochastic] = None
  ) = stochastic.option match {
    case None ⇒
      val integration: WorkflowIntegration.DeterministicGA[_] = WorkflowIntegration.DeterministicGA(
        DeterministicParams(
          mgo.niche.irregularGrid(objectives.map(_.scale).toVector),
          genome,
          objectives.map(_.p),
          operatorExploration),
        genome,
        objectives.map(_.p)
      )

      WorkflowIntegration.DeterministicGA.toEvolutionWorkflow(integration)
    case Some(stochastic) ⇒
      val integration: WorkflowIntegration.StochasticGA[_] = WorkflowIntegration.StochasticGA(
        StochasticParams(
          pattern = mgo.niche.irregularGrid(objectives.map(_.scale).toVector),
          aggregation = StochasticGAIntegration.aggregateVector(stochastic.aggregation, _),
          genome = genome,
          objectives = objectives.map(_.p),
          historySize = stochastic.replications,
          cloneProbability = stochastic.reevaluate,
          operatorExploration = operatorExploration
        ),
        genome,
        objectives.map(_.p),
        stochastic
      )

      WorkflowIntegration.StochasticGA.toEvolutionWorkflow(integration)
  }

}

