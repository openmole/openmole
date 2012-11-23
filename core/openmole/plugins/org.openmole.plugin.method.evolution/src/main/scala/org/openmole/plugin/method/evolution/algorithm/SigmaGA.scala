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
import java.security.spec.MGF1ParameterSpec
import org.openmole.core.implementation.tools.GroovyContextAdapter

object SigmaGA {

  trait SigmaGAGenome extends G {
    type G = GAGenomeWithSigma
  }

  trait SGATermination extends Termination with TerminationManifest with MG with SigmaGAGenome {
    type MF = Rank with Diversity
  }

  def counter(_steps: Int) =
    new CounterTermination with SGATermination {
      val steps = _steps
      val stateManifest = manifest[STATE]
    }

  def timed(_duration: Long) =
    new TimedTermination {
      val duration = _duration
      val stateManifest = manifest[STATE]
    }

  trait SGADiversityMetric extends DiversityMetric {
    type DIVERSIFIED = MGFitness
  }

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

  trait SGAArchive extends Archive with ArchiveManifest with SigmaGAGenome with MG

  def noArchive = new NoArchive with SGAArchive {
    val aManifest = manifest[A]
  }

  trait SGAAggregation extends Aggregation with MG
  trait SGAPlotter extends Plotter with SigmaGAGenome with MG

  def mapArchive(
    _plotter: SGAPlotter,
    _aggregation: SGAAggregation) = new MapArchive with SGAArchive {
    val aManifest = manifest[A]
    def plot(i: Individual[G, F]) = _plotter.plot(i)
    def aggregate(fitness: F) = _aggregation.aggregate(fitness)
  }

  def max = new MaxAggregation with SGAAggregation {}

  def genomePlotter(_x: Int, _y: Int, _nX: Int = 100, _nY: Int = 100) = new GenomePlotter with SGAPlotter {
    val x = _x
    val y = _y
    val nX = _nX
    val nY = _nY
  }

  def apply(
    distributionIndex: Double,
    mu: Int,
    lambda: Int,
    dominance: Dominance,
    termination: SigmaGA.SGATermination,
    diversityMetric: SigmaGA.DiversityMetricBuilder,
    archiving: SigmaGA.SGAArchive = SigmaGA.noArchive) =
    (genomeSize: Int) ⇒ new SigmaGA(
      distributionIndex,
      genomeSize,
      mu,
      lambda,
      dominance,
      termination,
      diversityMetric,
      archiving)

}

sealed class SigmaGA(
  val distributionIndex: Double,
  val genomeSize: Int,
  val mu: Int,
  val lambda: Int,
  val dominance: Dominance,
  val termination: SigmaGA.SGATermination,
  val diversityMetric: SigmaGA.DiversityMetricBuilder,
  val archiving: SigmaGA.SGAArchive = SigmaGA.noArchive) extends NSGAIISigma
    with BinaryTournamentSelection
    with NonDominatedElitism
    with CoEvolvingSigmaValuesMutation
    with SBXBoundedCrossover
    with ParetoRanking
    with RankDiversityModifier
    with EvolutionManifest
    with TerminationManifest {

  type STATE = termination.STATE
  type A = archiving.A

  val gManifest = manifest[G]
  val individualManifest = manifest[Individual[G, F]]
  val populationManifest = manifest[Population[G, F, MF]]
  val aManifest = archiving.aManifest
  val fManifest = manifest[F]
  val stateManifest = termination.stateManifest

  def initialState(p: Population[G, F, MF]): STATE = termination.initialState(p)
  def terminated(population: Population[G, F, MF], terminationState: STATE): (Boolean, STATE) = termination.terminated(population, terminationState)
  def diversity(individuals: Seq[DIVERSIFIED], ranks: Seq[Lazy[Int]]): Seq[Lazy[Double]] = diversityMetric(dominance).diversity(individuals, ranks)
  def isDominated(p1: Seq[scala.Double], p2: Seq[scala.Double]) = dominance.isDominated(p1, p2)
  def toArchive(individuals: Seq[Individual[G, F]]) = archiving.toArchive(individuals)
  def combine(a1: A, a2: A) = archiving.combine(a1, a2)
  def initialArchive = archiving.initialArchive
}