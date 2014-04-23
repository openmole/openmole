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
import org.openmole.core.model.data.Prototype

object ABC {

  trait ABC {
    def targetPrototypes: Seq[Prototype[Double]]
    def priorPrototypes: Seq[Prototype[Double]]
  }

  def jabotMover = new JabotMover {}

  def lenormand(
    priors: Seq[((Prototype[Double], (Double, Double)))],
    targets: Seq[(Prototype[Double], Double)],
    simulations: Int,
    minimumProportionOfAccepted: Double = 0.05,
    alpha: Double = 0.5,
    mover: ParticleMover = jabotMover) = {
    val (_priors, _simulations, _alpha, _targets, _minimumProportionOfAccepted) = (priors, simulations, alpha, targets, minimumProportionOfAccepted)

    new Lenormand with DefaultDistance with ABC.ABC {
      val targetPrototypes = _targets.unzip._1
      val priorPrototypes = _priors.unzip._1
      override val minimumProportionOfAccepted = _minimumProportionOfAccepted

      override val alpha = _alpha
      def move(simulations: Seq[WeightedSimulation])(implicit rng: Random) = mover.move(simulations)

      val summaryStatsTarget = targets.unzip._2
      val simulations = _simulations
      val priors = _priors.unzip._2.map { case (min, max) ⇒ Uniform(min, max) }
    }
  }

}
