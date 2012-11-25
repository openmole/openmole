/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.method.evolution.algorithm

import fr.iscpif.mgo._
import fr.iscpif.mgo.tools.Lazy
import java.util.Random

object SigmaGA {

  trait SGA extends G with MG with MF with RankDiversityMF with GASigma {
    type MF <: Rank with Diversity
    type RANKED = MGFitness
    type DIVERSIFIED = MGFitness
    val gManifest = manifest[G]
    val individualManifest = manifest[Individual[G, F]]
    val populationManifest = manifest[Population[G, F, MF]]
    val fManifest = manifest[F]
  }

  trait SGATermination extends Termination with TerminationManifest with MG with SGA

  def counter(_steps: Int) =
    new CounterTermination with SGATermination {
      val steps = _steps
      val stateManifest = manifest[STATE]
    }

  def timed(_duration: Long) =
    new TimedTermination with SGATermination {
      val duration = _duration
      val stateManifest = manifest[STATE]
    }

  trait SGARanking extends Ranking with SGA

  trait SGARankingBuilder {
    def apply(dominance: Dominance): SGARanking
  }

  def pareto = new SGARankingBuilder {
    def apply(_dominance: Dominance) = new ParetoRanking with SGARanking {
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = _dominance.isDominated(p1, p2)
    }
  }

  trait SGADiversityMetric extends DiversityMetric with SGA

  trait DiversityMetricBuilder {
    def apply(dominance: Dominance): SGADiversityMetric
  }

  def crowding = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new CrowdingDiversity with SGADiversityMetric
  }

  def hypervolume(_referencePoint: Seq[Double]) = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new HypervolumeDiversity with SGADiversityMetric {
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = dominance.isDominated(p1, p2)
      val referencePoint = _referencePoint
    }
  }

  def epsilon(_epsilons: Seq[Double]) = new EpsilonDominance {
    val epsilons = _epsilons
  }

  def strict = new StrictDominance {}
  def nonStrict = new NonStrictDominance {}

  trait SGAModifier extends Modifier with SGA
  trait SGAArchive extends Archive with ArchiveManifest with SGA with SGAModifier {
    val diversityMetric: SGADiversityMetric
    val ranking: SGARanking
    def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = diversityMetric.diversity(individuals, ranks)
    def rank(individuals: Seq[RANKED]) = ranking.rank(individuals)

  }

  trait SGAArchiveBuilder extends A {
    def apply(diversityMetric: SGADiversityMetric, ranking: SGARanking): SGAArchive
  }

  def noArchive = new SGAArchiveBuilder {
    def apply(_diversityMetric: SGADiversityMetric, _ranking: SGARanking) =
      new NoArchive with RankDiversityModifier with SGAArchive {
        override type DIVERSIFIED = MGFitness
        override type RANKED = MGFitness
        val aManifest = manifest[A]
        val diversityMetric = _diversityMetric
        val ranking = _ranking
      }
  }

  def mapArchive(_plotter: SGAPlotter, _aggregation: SGAAggregation, _neighbors: Int = 8) =
    new SGAArchiveBuilder {
      def apply(_diversityMetric: SGADiversityMetric, _ranking: SGARanking) =
        new MapArchive with SGAArchive with MapModifier {
          override type DIVERSIFIED = MGFitness
          override type RANKED = MGFitness
          val aManifest = manifest[A]
          def plot(i: Individual[G, F]) = _plotter.plot(i)
          def aggregate(fitness: F) = _aggregation.aggregate(fitness)
          val diversityMetric = _diversityMetric
          val ranking = _ranking
          val neighbors = _neighbors
        }
    }

  trait SGAAggregation extends Aggregation with MG
  trait SGAPlotter extends Plotter with SGA with MG

  def max = new MaxAggregation with SGAAggregation {}

  def genomePlotter(_x: Int, _y: Int, _nX: Int = 100, _nY: Int = 100) = new GenomePlotter with SGAPlotter {
    val x = _x
    val y = _y
    val nX = _nX
    val nY = _nY
  }

  trait SGACrossover extends CrossOver with SGA

  trait SGACrossoverBuilder {
    def apply(genomeSize: Factory[SGA#G]): SGACrossover
  }

  def sbx(_distributionIndex: Double = 2.0) = new SGACrossoverBuilder {
    def apply(_genomeFactory: Factory[SGA#G]) =
      new SBXBoundedCrossover with SGACrossover {
        val distributionIndex = _distributionIndex
        val genomeFactory = _genomeFactory
      }
  }

  trait SGAMutation extends Mutation with SGA

  trait SGAMutationBuilder extends SGA {
    def apply(genomeFactory: Factory[SGA#G]): SGAMutation
  }

  def coEvolvingSigma = new SGAMutationBuilder {
    def apply(_genomeFactory: Factory[SGA#G]) = new CoEvolvingSigmaValuesMutation with SGAMutation {
      val genomeFactory = _genomeFactory
    }
  }

  def apply(
    mu: Int,
    lambda: Int,
    termination: SigmaGA.SGATermination,
    mutation: SigmaGA.SGAMutationBuilder = coEvolvingSigma,
    crossover: SigmaGA.SGACrossoverBuilder = sbx(),
    dominance: Dominance = SigmaGA.strict,
    diversityMetric: SigmaGA.DiversityMetricBuilder = SigmaGA.crowding,
    ranking: SigmaGA.SGARankingBuilder = SigmaGA.pareto,
    archiving: SigmaGA.SGAArchiveBuilder = SigmaGA.noArchive) =
    new SigmaGA(mu, lambda, termination, mutation, crossover, dominance, diversityMetric, ranking, archiving)(_)

}

sealed class SigmaGA(
  val mu: Int,
  val lambda: Int,
  val termination: SigmaGA.SGATermination,
  val mutation: SigmaGA.SGAMutationBuilder = SigmaGA.coEvolvingSigma,
  val crossover: SigmaGA.SGACrossoverBuilder = SigmaGA.sbx(),
  val dominance: Dominance = SigmaGA.strict,
  val diversityMetric: SigmaGA.DiversityMetricBuilder = SigmaGA.crowding,
  val ranking: SigmaGA.SGARankingBuilder = SigmaGA.pareto,
  val archiving: SigmaGA.SGAArchiveBuilder = SigmaGA.noArchive)(val genomeSize: Int)
    extends MuPlusLambda
    with GASigmaFactory
    with BinaryTournamentSelection
    with NonDominatedElitism
    with EvolutionManifest
    with TerminationManifest
    with SigmaGA.SGA
    with Archive
    with Termination
    with Breeding
    with MG
    with Elitism
    with Modifier { sga ⇒

  lazy val thisRanking = ranking(dominance)
  lazy val thisDiversityMetric = diversityMetric(dominance)
  lazy val thisArchiving = archiving(thisDiversityMetric, thisRanking)
  lazy val thisCrossover = crossover(genomeFactory)
  lazy val thisMutation = mutation(genomeFactory)

  type STATE = termination.STATE
  type A = thisArchiving.A

  implicit val aManifest = thisArchiving.aManifest

  implicit val stateManifest = termination.stateManifest

  def initialState(p: Population[G, F, MF]): STATE = termination.initialState(p)
  def terminated(population: Population[G, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
  def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = thisDiversityMetric.diversity(individuals, ranks)
  def isDominated(p1: Seq[scala.Double], p2: Seq[scala.Double]) = dominance.isDominated(p1, p2)
  def toArchive(individuals: Seq[Individual[G, F]]) = thisArchiving.toArchive(individuals)
  def combine(a1: A, a2: A) = thisArchiving.combine(a1, a2)
  def initialArchive = thisArchiving.initialArchive
  def modify(individuals: Seq[Individual[G, F]], archive: A) = thisArchiving.modify(individuals, archive)
  def crossover(g1: G, g2: G)(implicit aprng: Random) = thisCrossover.crossover(g1, g2)
  def mutate(genome: G)(implicit aprng: Random) = thisMutation.mutate(genome)
}