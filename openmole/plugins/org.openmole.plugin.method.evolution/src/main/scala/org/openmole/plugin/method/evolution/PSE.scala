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

import mgo.algorithm.{ noisypse, pse }
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._
import org.openmole.tool.random._
import monocle.macros._

object PSE {

  object PatternAxe {

    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit fix: Fix[D, Double]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit fix: Fix[D, Int]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit fix: Fix[D, Long]): PatternAxe =
      PatternAxe(f.prototype, fix(f.domain).toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective, scale: Vector[Double])

  case class DeterministicParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    genomeSize:          Int,
    operatorExploration: Double
  )

  object DeterministicParams {

    import mgo.algorithm._
    import mgo.algorithm.pse._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] { api ⇒
      type G = mgo.algorithm.pse.Genome
      type I = Individual
      type S = EvolutionState[HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.algorithm.pse.PSE.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.pse.PSE(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: HitMapM, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← pse.state[M]
        } yield (newState, t)
      }

      def initialGenomes(om: DeterministicParams)(n: Int): M[Vector[G]] = interpret { impl ⇒
        import impl._
        zipWithState(pse.initialGenomes[DSL](n, om.genomeSize)).eval
      }

      def breeding(om: DeterministicParams)(individuals: Vector[Individual], n: Int) = interpret { impl ⇒
        import impl._
        zipWithState(pse.breeding[DSL](n, om.pattern, om.operatorExploration).run(individuals)).eval
      }

      def elitism(om: DeterministicParams)(individuals: Vector[Individual]) = interpret { impl ⇒
        import impl._
        zipWithState(pse.elitism[DSL](om.pattern).run(individuals)).eval
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
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = vectorPhenotype.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = pse.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[HitMapState](random = rng, s = Map())
        def initialGenomes(n: Int) = api.initialGenomes(om)(n)
        def breeding(individuals: Vector[Individual], n: Int) = api.breeding(om)(individuals, n)
        def elitism(individuals: Vector[Individual]) = api.elitism(om)(individuals)
        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def migrateToIsland(population: Vector[I]) = population.map(Individual.foundedIsland.set(true))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(i ⇒ !Individual.foundedIsland.get(i)).map(Individual.mapped.set(false))
      }

    }
  }

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe]
  ) = {
    val ug = UniqueGenome(genome)

    WorkflowIntegration.DeterministicGA(
      DeterministicParams(mgo.algorithm.pse.irregularGrid(objectives.map(_.scale).toVector), UniqueGenome.size(ug), operatorExploration),
      ug,
      objectives.map(_.p)
    )
  }

  case class StochasticParams(
    pattern:             Vector[Double] ⇒ Vector[Int],
    aggregation:         Vector[Vector[Double]] ⇒ Vector[Double],
    genomeSize:          Int,
    historySize:         Int,
    cloneProbability:    Double,
    operatorExploration: Double
  )

  object StochasticParams {

    import mgo.algorithm._
    import mgo.algorithm.noisypse._
    import cats.data._
    import freedsl.dsl._
    import mgo.contexts._

    implicit def integration = new MGOAPI.Integration[StochasticParams, Vector[Double], Vector[Double]] with MGOAPI.Stochastic { api ⇒
      type G = mgo.algorithm.noisypse.Genome
      type I = Individual
      type S = EvolutionState[pse.HitMapState]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      private def interpret[U](f: mgo.algorithm.pse.PSE.PSEImplicits ⇒ (S, U)) = State[S, U] { (s: S) ⇒
        mgo.algorithm.noisypse.NoisyPSE(s)(f)
      }

      private def zipWithState[M[_]: cats.Monad: StartTime: Random: Generation: pse.HitMapM, T](op: M[T]): M[(S, T)] = {
        import cats.implicits._
        for {
          t ← op
          newState ← noisypse.state[M]
        } yield (newState, t)
      }

      def initialGenomes(om: StochasticParams)(n: Int) = interpret { impl ⇒
        import impl._
        zipWithState(noisypse.initialGenomes[DSL](n, om.genomeSize)).eval
      }

      def breeding(om: StochasticParams)(individuals: Vector[Individual], n: Int) = interpret { impl ⇒
        import impl._
        zipWithState(
          noisypse.breeding[DSL](
            lambda = n,
            operatorExploration = om.operatorExploration,
            cloneProbability = om.cloneProbability,
            aggregation = om.aggregation,
            pattern = om.pattern
          ).run(individuals)
        ).eval
      }

      def elitism(om: StochasticParams)(individuals: Vector[Individual]) = interpret { impl ⇒
        import impl._
        zipWithState(
          noisypse.elitism[DSL](
            pattern = om.pattern,
            historySize = om.historySize,
            aggregation = om.aggregation
          ).run(individuals)
        ).eval
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
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = om.aggregation(vectorPhenotype.get(individual))
        def buildIndividual(genome: G, phenotype: Vector[Double]) = noisypse.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[pse.HitMapState](random = rng, s = Map())

        def initialGenomes(n: Int) = api.initialGenomes(om)(n)
        def breeding(individuals: Vector[Individual], n: Int) = api.breeding(om)(individuals, n)
        def elitism(individuals: Vector[Individual]) = api.elitism(om)(individuals)

        def afterGeneration(g: Long, population: Vector[I]) = api.afterGeneration(g, population)
        def afterDuration(d: squants.Time, population: Vector[I]) = api.afterDuration(d, population)

        def migrateToIsland(population: Vector[I]) =
          population.map(Individual.foundedIsland.set(true)).map(Individual.historyAge.set(0))

        def migrateFromIsland(population: Vector[I]) =
          population.filter(_.historyAge != 0).map {
            i ⇒
              val i1 = Individual.phenotypeHistory.modify(_.take(math.min(i.historyAge, om.historySize).toInt))(i)
              if (Individual.foundedIsland.get(i1))
                (Individual.mapped.set(true) andThen Individual.foundedIsland.set(false))(i1)
              else Individual.mapped.set(false)(i1)
          }
      }

      def samples(i: I): Long = i.phenotypeHistory.size
    }
  }

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe],
    stochastic: Stochastic[Seq]
  ) = {
    val ug = UniqueGenome(genome)

    WorkflowIntegration.StochasticGA(
      StochasticParams(
        pattern = mgo.algorithm.pse.irregularGrid(objectives.map(_.scale).toVector),
        aggregation = StochasticGAIntegration.aggregateVector(stochastic.aggregation, _),
        genomeSize = UniqueGenome.size(ug),
        historySize = stochastic.replications,
        cloneProbability = stochastic.reevaluate,
        operatorExploration = operatorExploration
      ),
      ug,
      objectives.map(_.p),
      stochastic
    )
  }

}

