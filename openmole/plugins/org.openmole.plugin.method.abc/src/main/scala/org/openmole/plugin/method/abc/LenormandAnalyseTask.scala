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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.dsl._
import fr.iscpif.scalabc._
import org.openmole.core.workflow.task._

object LenormandAnalyseTask {

  def apply(
    lenormand:  algorithm.Lenormand with ABC,
    state:      Prototype[algorithm.Lenormand#STATE],
    terminated: Prototype[Boolean],
    iteration:  Prototype[Int],
    accepted:   Prototype[Double]
  ) =
    ClosureTask("LenormandAnalyseTask") { (context, _, _) ⇒
      val thetasValue: Seq[Seq[Double]] = lenormand.priorPrototypes.map { p ⇒ context(p.toArray).toSeq }.transpose
      val summaryStatsValue: Seq[Seq[Double]] = lenormand.targetPrototypes.map { p ⇒ context(p.toArray).toSeq }.transpose
      val stateValue: algorithm.Lenormand#STATE = context(state)
      val nextState = lenormand.analyse(stateValue, thetasValue, summaryStatsValue)
      Context(
        Variable(state, nextState),
        Variable(terminated, lenormand.finished(nextState)),
        Variable(iteration, nextState.iteration),
        Variable(accepted, nextState.proportionOfAccepted)
      )
    } set (
      inputs += state,
      inputs += (lenormand.priorPrototypes: _*),
      outputs += (lenormand.targetPrototypes: _*),
      outputs += (state, terminated, iteration, accepted)
    )

}

