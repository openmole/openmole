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
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.script._
import org.openmole.misc.tools.service.Duration._
import scala.util.Random
import scalaz._

object GA {

  trait GAType <: G with P with F with MF with ContextPhenotype with MG //with genome.GAGenomeWithSigmaType

  trait GA extends GAGenomeWithSigma with GAType {
    val gManifest = manifest[G]
    val individualManifest = manifest[Individual[G, P, F]]
    val fManifest = manifest[F]
  }

  trait SelfGA <: GAType {
    val self: fr.iscpif.mgo.GA with Sigma

    def values: Lens[self.G, Seq[Double]] = self.values
    def genome: Lens[self.G, Seq[Double]] = self.genome

    /** Size of the value part of the genome */
    def genomeSize: Int = self.genomeSize

    def sigma: Lens[self.G, Seq[Double]] = self.sigma

    def randomGenome(implicit rng: Random): self.G = self.randomGenome
  }

  /*trait SelfPopulation <: G with P with F with MF {
    type self <: fr.iscpif.mgo.G with fr.iscpif.mgo.F with fr.iscpif.mgo.MF with fr.iscpif.mgo.P

    type G = GA#G
    type P = GA#P
    type F = GA#F
    type MF = GA#MF
  }*/

  trait GATermination extends Termination with TerminationManifest with GAType

  def counter(_steps: Int) = new GATermination with CounterTermination {
    type MF = Any
    val steps = _steps
    val stateManifest = manifest[STATE]
  }

  def timed(_duration: String) = new GATermination with TimedTermination {
    type MF = Any
    val duration = _duration.toMilliSeconds
    val stateManifest = manifest[STATE]
  }

  trait GARanking extends Ranking with GAType

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

  trait GADiversityMetric extends DiversityMetric with GAType

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
    with ArchiveManifest
    with GA
    with GAModifier
    with Elitism
    with PopulationManifest
    with Selection
    with Termination
    with TerminationManifest
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

  def optimisation(
    mu: Int,
    termination: GATermination { type G >: Optimisation#G; type P >: Optimisation#P; type F >: Optimisation#F; type MF >: Optimisation#MF },
    dominance: Dominance = strict,
    ranking: GARankingBuilder = pareto,
    diversityMetric: DiversityMetricBuilder = crowding) = new GAAlgorithmBuilder {
    val (_mu, _dominance, _ranking, _diversityMetric) = (mu, dominance, ranking, diversityMetric)
    def apply(_genomeSize: Int) =
      new Optimisation {
        val genomeSize = _genomeSize
        val aManifest = manifest[A]
        val populationManifest = manifest[Population[G, P, F, MF]]
        val diversityMetric = _diversityMetric(dominance)
        val ranking = _ranking(dominance)
        val mu = _mu
        def diversity(individuals: Seq[Seq[Double]], ranks: Seq[Lazy[Int]]) = diversityMetric.diversity(individuals, ranks)
        def rank(individuals: Seq[Seq[Double]]) = ranking.rank(individuals)
        type STATE = termination.STATE
        implicit val stateManifest = termination.stateManifest
        def initialState: STATE = termination.initialState
        def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
      }
  }

  trait GAProfile extends GAType {
    def aggregation: GAAggregation
    def x: Int
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

  def genomeProfile(
    x: Int,
    nX: Int,
    aggregation: GAAggregation,
    termination: GATermination { type G >: GenomeProfile#G; type P >: GenomeProfile#P; type F >: GenomeProfile#F; type MF >: GenomeProfile#MF }) = {
    val (_x, _nX, _aggregation) = (x, nX, aggregation)
    new GAAlgorithmBuilder with GAProfile {
      val aggregation = _aggregation
      val x = _x

      def apply(_genomeSize: Int) =
        new GenomeProfile {
          val genomeSize = _genomeSize
          val aManifest = manifest[A]
          val populationManifest = manifest[Population[G, P, F, MF]]
          val x = _x
          val nX = _nX
          def aggregate(fitness: F) = _aggregation.aggregate(fitness)
          type STATE = termination.STATE
          implicit val stateManifest = termination.stateManifest
          def initialState: STATE = termination.initialState
          def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
        }
    }
  }

  trait GAProfilePlotter extends ProfilePlotter with GAType with MG

  def profilePlotter(x: String) = new GAProfilePlotter {
    @transient lazy val interpreter = new GroovyProxyPool(x)

    def plot(individual: Individual[this.type#G, this.type#P, this.type#F]) =
      interpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt

  }

  trait GAMap extends GAType {
    def aggregation: GAAggregation
    def x: Int
    def y: Int
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

  def genomeMap(
    x: Int,
    nX: Int,
    y: Int,
    nY: Int,
    aggregation: GAAggregation,
    termination: GATermination { type G >: GenomeMap#G; type P >: GenomeMap#P; type F >: GenomeMap#F; type MF >: GenomeMap#MF }) = {
    val (_x, _nX, _y, _nY, _aggregation) = (x, nX, y, nY, aggregation)
    new GAAlgorithmBuilder with GAMap {
      val aggregation = _aggregation
      val x = _x
      val y = _y

      def apply(_genomeSize: Int) =
        new GenomeMap {
          val genomeSize = _genomeSize
          val aManifest = manifest[A]
          val populationManifest = manifest[Population[G, P, F, MF]]

          def aggregate(fitness: F) = _aggregation.aggregate(fitness)
          val x = _x
          val y = _y
          val nX = _nX
          val nY = _nY

          type STATE = termination.STATE
          implicit val stateManifest = termination.stateManifest
          def initialState: STATE = termination.initialState
          def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)

        }
    }
  }

  trait GAAggregation extends Aggregation with MG
  trait GAMapPlotter extends MapPlotter with GAType with MG

  def max = new MaxAggregation with GAAggregation {}

  def mapGenomePlotter(x: String, y: String) = new GAMapPlotter {
    @transient lazy val xInterpreter = new GroovyProxyPool(x)
    @transient lazy val yInterpreter = new GroovyProxyPool(y)

    def plot(individual: Individual[this.type#G, this.type#P, this.type#F]) =
      (xInterpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt,
        yInterpreter.execute(individual.phenotype.toBinding).asInstanceOf[Double].toInt)
  }

  def apply(
    algorithm: GAAlgorithmBuilder,
    lambda: Int,
    cloneProbability: Double = 0.0) =
    new org.openmole.plugin.method.evolution.algorithm.GAImpl(algorithm, lambda, cloneProbability)(_)

}

trait GA extends GAGenomeWithSigma
  with EvolutionManifest
  with TerminationManifest
  with GA.GA
  with Archive
  with Termination
  with GeneticBreeding
  with MG
  with Elitism
  with Modifier
  with CloneRemoval
  with ContextPhenotype
  with PopulationManifest

sealed class GAImpl(
  val algorithm: GA.GAAlgorithmBuilder,
  val lambda: Int,
  override val cloneProbability: Double)(val genomeSize: Int)
    extends GA { sga ⇒

  lazy val thisAlgorithm = algorithm.apply(genomeSize)

  type STATE = thisAlgorithm.STATE
  type A = thisAlgorithm.A
  type MF = thisAlgorithm.MF

  implicit val aManifest = thisAlgorithm.aManifest
  implicit val populationManifest = thisAlgorithm.populationManifest

  implicit val stateManifest = thisAlgorithm.stateManifest
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