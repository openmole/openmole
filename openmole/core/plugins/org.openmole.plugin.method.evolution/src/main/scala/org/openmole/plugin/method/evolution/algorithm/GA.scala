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
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.script._

object GA {

  trait GA extends G with ContextPhenotype with MG with MF with RankDiversityMF with GASigma {
    type MF <: Rank with Diversity
    type RANKED = MGFitness
    type DIVERSIFIED = MGFitness
    val gManifest = manifest[G]
    val individualManifest = manifest[Individual[G, P, F]]
    val populationManifest = manifest[Population[G, P, F, MF]]
    val fManifest = manifest[F]
  }

  trait GATermination extends Termination with TerminationManifest with GA

  def counter(_steps: Int) =
    new CounterTermination with GATermination {
      val steps = _steps
      val stateManifest = manifest[STATE]
    }

  def timed(_duration: Long) =
    new TimedTermination with GATermination {
      val duration = _duration
      val stateManifest = manifest[STATE]
    }

  trait GARanking extends Ranking with GA

  trait GARankingBuilder {
    def apply(dominance: Dominance): GARanking
  }

  def pareto = new GARankingBuilder {
    def apply(_dominance: Dominance) = new ParetoRanking with GARanking {
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = _dominance.isDominated(p1, p2)
    }
  }

  trait GADiversityMetric extends DiversityMetric with GA

  trait DiversityMetricBuilder {
    def apply(dominance: Dominance): GADiversityMetric
  }

  def crowding = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new CrowdingDiversity with GADiversityMetric
  }

  def hypervolume(_referencePoint: Double*) = new DiversityMetricBuilder {
    def apply(dominance: Dominance) = new HypervolumeDiversity with GADiversityMetric {
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = dominance.isDominated(p1, p2)
      val referencePoint = _referencePoint
    }
  }

  def strictEpsilon(_epsilons: Double*) = new StrictEpsilonDominance {
    val epsilons = _epsilons
  }

  def nonStrictEpsilon(_epsilons: Double*) = new NonStrictEpsilonDominance {
    val epsilons = _epsilons
  }

  def strict = new StrictDominance {}
  def nonStrict = new NonStrictDominance {}

  trait GAModifier extends Modifier with GA

  trait GAAlgorithm extends Archive with ArchiveManifest with GA with GAModifier with Elitism {
    val diversityMetric: GADiversityMetric
    val ranking: GARanking
    def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = diversityMetric.diversity(individuals, ranks)
    def rank(individuals: Seq[RANKED]) = ranking.rank(individuals)
  }

  trait GAAlgorithmBuilder extends A {
    def apply(diversityMetric: GADiversityMetric, ranking: GARanking): GAAlgorithm
  }

  def optimization(_mu: Int) = new GAAlgorithmBuilder {
    def apply(_diversityMetric: GADiversityMetric, _ranking: GARanking) =
      new NoArchive with RankDiversityModifier with GAAlgorithm with NonDominatedElitism {
        override type DIVERSIFIED = MGFitness
        override type RANKED = MGFitness
        val aManifest = manifest[A]
        val diversityMetric = _diversityMetric
        val ranking = _ranking
        val mu = _mu
      }
  }

  trait GAProfile extends GA {
    def aggregation: GAAggregation
    def x: Int
  }

  def profile(_x: Int, _nX: Int, _worst: Double, _aggregation: GAAggregation) =
    new GAAlgorithmBuilder with GAProfile {
      val aggregation = _aggregation
      val x = _x

      def apply(_diversityMetric: GADiversityMetric, _ranking: GARanking) =
        new GAAlgorithm with ProfileModifier with ProfileElitism with NoArchive with ProfileGenomePlotter {
          override type DIVERSIFIED = MGFitness
          override type RANKED = MGFitness
          val aManifest = manifest[A]
          val x = _x
          val nX = _nX
          val worst = _worst
          def aggregate(fitness: F) = _aggregation.aggregate(fitness)
          val diversityMetric = _diversityMetric
          val ranking = _ranking
        }
    }

  trait GAProfilePlotter extends ProfilePlotter with GA with MG

  def profilePlotter(x: String) = new GAProfilePlotter {
    @transient lazy val interpreter = new GroovyProxyPool(x)

    def plot(individual: Individual[this.type#G, this.type#P, this.type#F]) =
      interpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt

  }

  def map(_plotter: GAMapPlotter, _aggregation: GAAggregation, neighbors: Int = 8) = {
    val (_neighbors) = (neighbors)
    new GAAlgorithmBuilder {
      def apply(_diversityMetric: GADiversityMetric, _ranking: GARanking) =
        new MapArchive with GAAlgorithm with MapModifier with MapElitism {
          override type DIVERSIFIED = MGFitness
          override type RANKED = MGFitness
          val aManifest = manifest[A]
          def plot(i: Individual[G, P, F]) = _plotter.plot(i)
          def aggregate(fitness: F) = _aggregation.aggregate(fitness)
          val diversityMetric = _diversityMetric
          val ranking = _ranking
          val neighbors = _neighbors
        }
    }
  }

  trait GAAggregation extends Aggregation with MG
  trait GAMapPlotter extends MapPlotter with GA with MG

  def max = new MaxAggregation with GAAggregation {}

  def mapGenomePlotter(x: String, y: String) = new GAMapPlotter {
    @transient lazy val xInterpreter = new GroovyProxyPool(x)
    @transient lazy val yInterpreter = new GroovyProxyPool(y)

    def plot(individual: Individual[this.type#G, this.type#P, this.type#F]) =
      (xInterpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt,
        yInterpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt)
  }

  trait GACrossover extends CrossOver with GA

  trait GACrossoverBuilder {
    def apply(genomeSize: Factory[GA#G]): GACrossover
  }

  def sbx(_distributionIndex: Double = 2.0) = new GACrossoverBuilder {
    def apply(_genomeFactory: Factory[GA#G]) =
      new SBXBoundedCrossover with GACrossover {
        val distributionIndex = _distributionIndex
        val genomeFactory = _genomeFactory
      }
  }

  trait GAMutation extends Mutation with GA

  trait GAMutationBuilder extends GA {
    def apply(genomeFactory: Factory[GA#G]): GAMutation
  }

  def coEvolvingSigma = new GAMutationBuilder {
    def apply(_genomeFactory: Factory[GA#G]) = new CoEvolvingSigmaValuesMutation with GAMutation {
      val genomeFactory = _genomeFactory
    }
  }

  def apply(
    algorithm: GAAlgorithmBuilder,
    lambda: Int,
    termination: GATermination,
    mutation: GAMutationBuilder = coEvolvingSigma,
    crossover: GACrossoverBuilder = sbx(),
    dominance: Dominance = strict,
    diversityMetric: DiversityMetricBuilder = crowding,
    ranking: GARankingBuilder = pareto,
    cloneProbability: Double = 0.0) =
    new org.openmole.plugin.method.evolution.algorithm.GAImpl(algorithm, lambda, termination, mutation, crossover, dominance, diversityMetric, ranking, cloneProbability)(_)

}

trait GA extends GASigmaFactory
  with BinaryTournamentSelection
  with EvolutionManifest
  with TerminationManifest
  with GA.GA
  with Archive
  with Termination
  with Breeding
  with MG
  with Elitism
  with Modifier
  with CloneRemoval
  with ContextPhenotype

sealed class GAImpl(
  val algorithm: GA.GAAlgorithmBuilder,
  val lambda: Int,
  val termination: GA.GATermination,
  val mutation: GA.GAMutationBuilder,
  val crossover: GA.GACrossoverBuilder,
  val dominance: Dominance,
  val diversityMetric: GA.DiversityMetricBuilder,
  val ranking: GA.GARankingBuilder,
  override val cloneProbability: Double)(val genomeSize: Int)
    extends GA { sga ⇒

  lazy val thisRanking = ranking(dominance)
  lazy val thisDiversityMetric = diversityMetric(dominance)
  lazy val thisAlgorithm = algorithm(thisDiversityMetric, thisRanking)
  lazy val thisCrossover = crossover(genomeFactory)
  lazy val thisMutation = mutation(genomeFactory)

  type STATE = termination.STATE
  type A = thisAlgorithm.A

  implicit val aManifest = thisAlgorithm.aManifest

  implicit val stateManifest = termination.stateManifest

  def initialState: STATE = termination.initialState
  def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
  def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = thisDiversityMetric.diversity(individuals, ranks)
  def isDominated(p1: Seq[scala.Double], p2: Seq[scala.Double]) = dominance.isDominated(p1, p2)
  def toArchive(individuals: Seq[Individual[G, P, F]]) = thisAlgorithm.toArchive(individuals)
  def combine(a1: A, a2: A) = thisAlgorithm.combine(a1, a2)
  def diff(a1: A, a2: A) = thisAlgorithm.diff(a1, a2)
  def initialArchive = thisAlgorithm.initialArchive
  def modify(individuals: Seq[Individual[G, P, F]], archive: A) = thisAlgorithm.modify(individuals, archive)
  def crossover(g1: G, g2: G)(implicit aprng: Random) = thisCrossover.crossover(g1, g2)
  def mutate(genome: G)(implicit aprng: Random) = thisMutation.mutate(genome)
  def elitism(individuals: Seq[Individual[G, P, F]], a: A) = thisAlgorithm.elitism(individuals, a)
}