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

package org.openmole.plugin.method.abc

import org.openmole.core.workflow.data.Prototype
import fr.iscpif.scalabc._

object Lenormand {
  def apply(
    priors: Seq[((Prototype[Double], (Double, Double)))],
    targets: Seq[(Prototype[Double], Double)],
    simulations: Int,
    minimumProportionOfAccepted: Double = 0.05,
    alpha: Double = 0.5) = {
    val (_priors, _simulations, _alpha, _targets, _minimumProportionOfAccepted) = (priors, simulations, alpha, targets, minimumProportionOfAccepted)

    new algorithm.Lenormand with sampling.JabotMover with distance.DefaultDistance with ABC {
      val targetPrototypes = _targets.unzip._1
      val priorPrototypes = _priors.unzip._1
      override val minimumProportionOfAccepted = _minimumProportionOfAccepted
      override val alpha = _alpha
      val summaryStatsTarget = targets.unzip._2
      val simulations = _simulations
      val priors = _priors.unzip._2.map { case (min, max) ⇒ prior.Uniform(min, max) }
    }
  }
}
