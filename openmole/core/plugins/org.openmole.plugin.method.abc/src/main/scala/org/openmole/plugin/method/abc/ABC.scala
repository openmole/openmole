/*
 * Copyright (C) 15/01/14 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.method.abc

import fr.irstea.scalabc._
import algorithm._
import sampling._
import distance._
import prior._
import scala.util.Random

object ABC {

  def jabotMover = new JabotMover {}

  def lenormand(
    targets: Seq[Double],
    priors: Seq[(Double, Double)],
    simulations: Int,
    alpha: Double = 1.0,
    mover: ParticleMover = jabotMover) = {
    val (_priors, _simulations, _alpha) = (priors, simulations, alpha)

    new Lenormand with DefaultDistance {
      override def alpha = _alpha
      def move(simulations: Seq[WeightedSimulation])(implicit rng: Random) = mover.move(simulations)

      def summaryStatsTarget = targets
      def simulations = _simulations
      val priors = _priors.map { case (min, max) ⇒ Uniform(min, max) }
    }
  }

}
