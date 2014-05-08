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
import fr.iscpif.mgo.{ GA ⇒ MGOGA }
import fr.iscpif.mgo.tools.Lazy
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.script._
import org.openmole.misc.tools.service.Duration._
import scala.util.Random
import scalaz._

object GA {

  trait GAType <: G with P with F with MF with MG with MGOGA with Sigma with DoubleSequencePhenotype with MGFitness

  trait GATermination extends Termination with TerminationManifest

  def counter(_steps: Int) = new GATermination with CounterTermination {
    type G = Any
    type F = Any
    type P = Any
    type MF = Any
    val stateManifest: Manifest[STATE] = manifest[STATE]
    val steps = _steps
  }

  def timed(_duration: String) = new GATermination with TimedTermination {
    type G = Any
    type F = Any
    type P = Any
    type MF = Any
    val stateManifest: Manifest[STATE] = manifest[STATE]
    val duration = _duration.toMilliSeconds
  }

  trait GARanking extends Ranking

  trait GARankingBuilder {
    def apply(dominance: Dominance): GARanking
  }

  def pareto = new GARankingBuilder {
    def apply(dominance: Dominance) = new ParetoRanking with GARanking {
      def isDominated(p1: Seq[Double], p2: Seq[Double]) = dominance.isDominated(p1, p2)
    }
  }

  def hierarchical = new GARankingBuilder {
    def apply(dominance: Dominance) = new HierarchicalRanking with GARanking {}
  }

  trait GADiversityMetric extends DiversityMetric

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

  trait GAModifier extends Modifier with GAType

  trait GAAlgorithm extends Archive
    with EvolutionManifest
    with GAType
    with GAModifier
    with Elitism
    with Selection
    with Termination
    with Mutation
    with CrossOver

  trait GAAlgorithmBuilder extends A {
    def apply(genomeSize: Int): GAAlgorithm
  }

  trait Optimisation extends NoArchive
    with RankDiversityModifier
    with GAAlgorithm
    with NonDominatedElitism
    with BinaryTournamentSelection
    with TournamentOnRankAndDiversity
    with CoEvolvingSigmaValuesMutation
    with SBXBoundedCrossover
    with GAType
    with GAGenomeWithSigma

  def optimisation(
    mu: Int,
    termination: GATermination { type G >: Optimisation#G; type P >: Optimisation#P; type F >: Optimisation#F; type MF >: Optimisation#MF },
    dominance: Dominance = strict,
    ranking: GARankingBuilder = pareto,
    diversityMetric: DiversityMetricBuilder = crowding) = new GAAlgorithmBuilder {
    val (_mu, _dominance, _ranking, _diversityMetric) = (mu, dominance, ranking, diversityMetric)
    def apply(_genomeSize: Int) =
      new Optimisation {
        val stateManifest: Manifest[STATE] = termination.stateManifest
        val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
        val individualManifest: Manifest[Individual[G, P, F]] = implicitly
        val aManifest: Manifest[A] = implicitly
        val fManifest: Manifest[F] = implicitly
        val gManifest: Manifest[G] = implicitly

        val genomeSize = _genomeSize
        val diversityMetric = _diversityMetric(dominance)
        val ranking = _ranking(dominance)
        val mu = _mu
        def diversity(individuals: Seq[Seq[Double]], ranks: Seq[Lazy[Int]]) = diversityMetric.diversity(individuals, ranks)
        def rank(individuals: Seq[Seq[Double]]) = ranking.rank(individuals)
        type STATE = termination.STATE
        def initialState: STATE = termination.initialState
        def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
      }
  }

  trait GenomeProfile extends GAAlgorithm
      with ProfileModifier
      with ProfileElitism
      with NoArchive
      with NoDiversity
      with ProfileGenomePlotter
      with HierarchicalRanking
      with BinaryTournamentSelection
      with TournamentOnRank
      with CoEvolvingSigmaValuesMutation
      with SBXBoundedCrossover
      with GAType
      with GAGenomeWithSigma
      with MGFitness
      with MaxAggregation {
    def x: Int
  }

  def genomeProfile(
    x: Int,
    nX: Int,
    termination: GATermination { type G >: GenomeProfile#G; type P >: GenomeProfile#P; type F >: GenomeProfile#F; type MF >: GenomeProfile#MF }) = {
    val (_x, _nX) = (x, nX)
    new GAAlgorithmBuilder {
      val x = _x

      def apply(_genomeSize: Int) =
        new GenomeProfile {
          val stateManifest: Manifest[STATE] = termination.stateManifest
          val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
          val individualManifest: Manifest[Individual[G, P, F]] = implicitly
          val aManifest: Manifest[A] = implicitly
          val fManifest: Manifest[F] = implicitly
          val gManifest: Manifest[G] = implicitly

          val genomeSize = _genomeSize
          val x = _x
          val nX = _nX
          type STATE = termination.STATE
          def initialState: STATE = termination.initialState
          def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
        }
    }
  }

  trait GenomeMap extends GAAlgorithm
      with MapElitism
      with MapGenomePlotter
      with NoArchive
      with NoRanking
      with NoModifier
      with MapSelection
      with CoEvolvingSigmaValuesMutation
      with SBXBoundedCrossover
      with GAType
      with GAGenomeWithSigma
      with MaxAggregation {
    def x: Int
    def y: Int
  }

  def genomeMap(
    x: Int,
    nX: Int,
    y: Int,
    nY: Int,
    termination: GATermination { type G >: GenomeMap#G; type P >: GenomeMap#P; type F >: GenomeMap#F; type MF >: GenomeMap#MF }) = {
    val (_x, _nX, _y, _nY) = (x, nX, y, nY)
    new GAAlgorithmBuilder {
      val x = _x
      val y = _y

      def apply(_genomeSize: Int) =
        new GenomeMap {
          val stateManifest: Manifest[STATE] = termination.stateManifest
          val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
          val individualManifest: Manifest[Individual[G, P, F]] = implicitly
          val aManifest: Manifest[A] = implicitly
          val fManifest: Manifest[F] = implicitly
          val gManifest: Manifest[G] = implicitly

          val genomeSize = _genomeSize
          val x = _x
          val y = _y
          val nX = _nX
          val nY = _nY

          type STATE = termination.STATE

          def initialState: STATE = termination.initialState
          def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)

        }
    }
  }

  def apply[AG <: GAAlgorithmBuilder](
    algorithm: AG,
    lambda: Int,
    cloneProbability: Double = 0.0) = {
    val (_lambda, _cloneProbability) = (lambda, cloneProbability)

    s: Int ⇒
      new org.openmole.plugin.method.evolution.algorithm.GAImpl[AG](algorithm)(s) {
        val lambda = _lambda
        override val cloneProbability = _cloneProbability
      }
  }

}

trait GA[ALGO <: GA.GAAlgorithmBuilder] extends GA.GAType
    with Archive
    with Termination
    with GeneticBreeding
    with Elitism
    with Modifier
    with CloneRemoval
    with EvolutionManifest {
  def algorithm: ALGO
}

sealed abstract class GAImpl[ALGO <: GA.GAAlgorithmBuilder](val algorithm: ALGO)(val genomeSize: Int) extends GA[ALGO] { sga ⇒
  val lambda: Int

  lazy val thisAlgorithm = algorithm.apply(genomeSize)

  type STATE = thisAlgorithm.STATE
  type G = thisAlgorithm.G
  //type P = thisAlgorithm.P
  //type F = thisAlgorithm.F
  type MF = thisAlgorithm.MF
  type A = thisAlgorithm.A

  /*val stateManifest: Manifest[STATE] = implicitly
  val populationManifest: Manifest[Population[G, P, F, MF]] = implicitly
  val individualManifest: Manifest[Individual[G, P, F]] = implicitly
  val aManifest: Manifest[A] = implicitly
  val fManifest: Manifest[F] = implicitly
  val gManifest: Manifest[G] = implicitly*/

  implicit val stateManifest: Manifest[STATE] = thisAlgorithm.stateManifest
  implicit val populationManifest: Manifest[Population[G, P, F, MF]] = thisAlgorithm.populationManifest
  implicit val individualManifest: Manifest[Individual[G, P, F]] = thisAlgorithm.individualManifest
  implicit val aManifest: Manifest[A] = thisAlgorithm.aManifest
  implicit val fManifest: Manifest[F] = thisAlgorithm.fManifest
  implicit val gManifest: Manifest[G] = thisAlgorithm.gManifest

  val genome = thisAlgorithm.genome
  val values = thisAlgorithm.values
  val sigma = thisAlgorithm.sigma

  def randomGenome(implicit rng: Random) = thisAlgorithm.randomGenome

  def initialState: STATE = thisAlgorithm.initialState
  def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = thisAlgorithm.terminated(population, terminationState)
  def toArchive(individuals: Seq[Individual[G, P, F]]) = thisAlgorithm.toArchive(individuals)
  def combine(a1: A, a2: A) = thisAlgorithm.combine(a1, a2)
  def diff(a1: A, a2: A) = thisAlgorithm.diff(a1, a2)
  def initialArchive = thisAlgorithm.initialArchive
  def modify(individuals: Seq[Individual[G, P, F]], archive: A) = thisAlgorithm.modify(individuals, archive)
  def crossover(g1: G, g2: G, population: Seq[Individual[G, P, F]], archive: A)(implicit rng: Random) = thisAlgorithm.crossover(g1, g2, population, archive)
  def mutate(genome: G, population: Seq[Individual[G, P, F]], archive: A)(implicit rng: Random) = thisAlgorithm.mutate(genome, population, archive)
  def elitism(individuals: Seq[Individual[G, P, F]], newIndividuals: Seq[Individual[G, P, F]], a: A)(implicit rng: Random) = thisAlgorithm.elitism(individuals, newIndividuals, a)
  def selection(population: Population[G, P, F, MF])(implicit rng: Random): Iterator[Individual[G, P, F]] = thisAlgorithm.selection(population)

}