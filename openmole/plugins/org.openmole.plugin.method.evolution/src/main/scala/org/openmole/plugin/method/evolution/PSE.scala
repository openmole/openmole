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
import org.openmole.tool.random.RandomProvider

object PSE {

  object PatternAxe {

    // FIXME provide an evidence that the domain does'nt require a context
    implicit def fromDoubleDomainToPatternAxe[D](f: Factor[D, Double])(implicit finite: Finite[D, Double]): PatternAxe =
      PatternAxe(f.prototype, finite.computeValues(f.domain).from(Context.empty)(RandomProvider.empty).toVector)

    implicit def fromIntDomainToPatternAxe[D](f: Factor[D, Int])(implicit finite: Finite[D, Int]): PatternAxe =
      PatternAxe(f.prototype, finite.computeValues(f.domain).from(Context.empty)(RandomProvider.empty).toVector.map(_.toDouble))

    implicit def fromLongDomainToPatternAxe[D](f: Factor[D, Long])(implicit finite: Finite[D, Long]): PatternAxe =
      PatternAxe(f.prototype, finite.computeValues(f.domain).from(Context.empty)(RandomProvider.empty).toVector.map(_.toDouble))

  }

  case class PatternAxe(p: Objective, scale: Vector[Double])

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe]
  ) = {
    val ug = UniqueGenome(genome)

    WorkflowIntegration.DeterministicGA(
      pse.OpenMOLE(mgo.algorithm.pse.irregularGrid(objectives.map(_.scale).toVector), UniqueGenome.size(ug), operatorExploration),
      ug,
      objectives.map(_.p)
    )
  }

  def apply(
    genome:     Genome,
    objectives: Seq[PatternAxe],
    stochastic: Stochastic[Seq]
  ) = {
    val ug = UniqueGenome(genome)

    WorkflowIntegration.StochasticGA(
      noisypse.OpenMOLE(
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

