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

object GenomeProfile {

  def apply(
    x:         Val[Double],
    nX:        Int,
    genome:    Genome,
    objective: Objective
  ) =
    NichedNSGA2(
      Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
      genome,
      objectives = Seq(objective),
      nicheSize = 1
    )

  def apply(
    x:          Val[Double],
    nX:         Int,
    genome:     Genome,
    objective:  Objective,
    stochastic: Stochastic,
    nicheSize:  Int         = 20
  ) =
    NichedNSGA2(
      Vector(NichedNSGA2.NichedElement.Continuous(x, nX)),
      genome,
      Seq(objective),
      nicheSize,
      stochastic = stochastic
    )

}
