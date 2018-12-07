/*
 * Copyright (C) 2014 Romain Reuillon
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

import org.openmole.core.dsl._
import cats._
import org.openmole.core.context.Context
import org.openmole.core.workflow.tools.DefaultSet

object GenomeProfile {

  def apply(
    x:          Val[Double],
    nX:         Int,
    genome:     Genome,
    objective:  Objective[_],
    stochastic: OptionalArgument[Stochastic] = None,
    nicheSize:  Int                          = 20
  ) = {
    stochastic.option match {
      case None ⇒
        NichedNSGA2(
          Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
          genome,
          objectives = Seq(objective),
          nicheSize = 1
        )
      case Some(stochastic) ⇒
        NichedNSGA2(
          Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
          genome,
          Seq(objective),
          nicheSize,
          stochastic = stochastic
        )
    }
  }

}

object GenomeProfileEvolution {

  import org.openmole.core.dsl._
  import org.openmole.core.workflow.puzzle._

  def apply(
    x:            Val[Double],
    nX:           Int,
    genome:       Genome,
    objective:    Objective[_],
    evaluation:   Puzzle,
    termination:  OMTermination,
    nicheSize:    Int                                    = 20,
    stochastic:   OptionalArgument[Stochastic]           = None,
    parallelism:  Int                                    = 1,
    distribution: EvolutionPattern                       = SteadyState(),
    suggestion:   Seq[Seq[DefaultSet.DefaultAssignment]] = Seq()) =
    EvolutionPattern.build(
      algorithm =
        GenomeProfile(
          x = x,
          nX = nX,
          genome = genome,
          objective = objective,
          stochastic = stochastic
        ),
      evaluation = evaluation,
      termination = termination,
      stochastic = stochastic,
      parallelism = parallelism,
      distribution = distribution,
      suggestion = suggestion.map(DefaultSet.fromAssignments)
    )

}
