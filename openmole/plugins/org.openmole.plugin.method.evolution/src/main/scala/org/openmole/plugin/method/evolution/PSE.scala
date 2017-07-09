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
    import context._
    import context.implicits._

    implicit def integration = new MGOAPI.Integration[DeterministicParams, Vector[Double], Vector[Double]] {
      type M[A] = context.M[A]
      type G = mgo.algorithm.pse.Genome
      type I = Individual
      type S = EvolutionState[HitMap]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def mMonad = implicitly
      def mGeneration = implicitly
      def mStartTime = implicitly

      def operations(om: DeterministicParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = vectorPhenotype.get(individual)
        def buildIndividual(genome: G, phenotype: Vector[Double]) = pse.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[HitMap](random = rng, s = Map())
        def initialGenomes(n: Int): M[Vector[G]] = pse.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) = pse.breeding(n, om.pattern, om.operatorExploration)
        def elitism = pse.elitism(om.pattern)
        def migrateToIsland(population: Vector[I]) = population.map(Individual.foundedIsland.set(true))
        def migrateFromIsland(population: Vector[I]) =
          population.filter(i ⇒ !Individual.foundedIsland.get(i)).map(Individual.mapped.set(false))
      }

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← pse.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
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
    import context._
    import context.implicits._

    implicit def integration = new MGOAPI.Integration[StochasticParams, Vector[Double], Vector[Double]] with MGOAPI.Stochastic {
      type M[A] = context.M[A]
      type G = mgo.algorithm.noisypse.Genome
      type I = Individual
      type S = EvolutionState[noisypse.HitMap]

      def iManifest = implicitly
      def gManifest = implicitly
      def sManifest = implicitly

      def mMonad = implicitly
      def mGeneration = implicitly
      def mStartTime = implicitly

      def operations(om: StochasticParams) = new Ops {
        def randomLens = GenLens[S](_.random)
        def startTimeLens = GenLens[S](_.startTime)
        def generation(s: S) = s.generation
        def values(genome: G) = vectorValues.get(genome)
        def genome(i: I) = Individual.genome.get(i)
        def phenotype(individual: I): Vector[Double] = om.aggregation(vectorPhenotype.get(individual))
        def buildIndividual(genome: G, phenotype: Vector[Double]) = noisypse.buildIndividual(genome, phenotype)
        def initialState(rng: util.Random) = EvolutionState[noisypse.HitMap](random = rng, s = Map())
        def initialGenomes(n: Int) = noisypse.initialGenomes(n, om.genomeSize)
        def breeding(n: Int) =
          noisypse.breeding(
            lambda = n,
            operatorExploration = om.operatorExploration,
            cloneProbability = om.cloneProbability,
            aggregation = om.aggregation,
            pattern = om.pattern
          )

        def elitism =
          noisypse.elitism(
            pattern = om.pattern,
            historySize = om.historySize,
            aggregation = om.aggregation
          )

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

      def run[A](s: S, x: M[A]) = {
        val res =
          for {
            xv ← x
            s ← pse.state[M]
          } yield (s, xv)
        interpreter(s).run(res).right.get
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

