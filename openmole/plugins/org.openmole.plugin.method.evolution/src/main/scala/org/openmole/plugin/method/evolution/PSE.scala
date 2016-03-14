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

import fr.iscpif.mgo
import fr.iscpif.mgo.algorithm.{ pse, noisypse }
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.sampling._

object PSE {

  object PatternAxe {

    // FIXME provide an evidence that the domain does'nt require a context
    implicit def fromDomainToPatternAxe[D](f: Factor[D, Double])(implicit finite: Finite[D, Double]) =
      PatternAxe(f.prototype, finite.computeValues(f.domain).from(Context.empty)(RandomProvider.empty).toVector)

  }

  case class PatternAxe(p: Prototype[Double], scale: Vector[Double])

  def apply(
    genome: Genome,
    objectives: Seq[PatternAxe]) = {
    WorkflowIntegration.DeterministicGA(
      pse.OpenMOLE(mgo.niche.irregularGrid(objectives.map(_.scale).toVector), Genome.size(genome), operatorExploration),
      genome,
      objectives.map(_.p)
    )
  }

  def apply(
    genome: Genome,
    objectives: Seq[PatternAxe],
    replication: Replication[Seq]) = {

    WorkflowIntegration.StochasticGA(
      noisypse.OpenMOLE(
        pattern = mgo.niche.irregularGrid(objectives.map(_.scale).toVector),
        aggregation = StochasticGAIntegration.aggregateVector(replication.aggregationClosures, _),
        genomeSize = Genome.size(genome),
        historySize = replication.max,
        cloneProbability = replication.reevaluate,
        operatorExploration = operatorExploration
      ),
      genome,
      objectives.map(_.p),
      replication
    )
  }

}

