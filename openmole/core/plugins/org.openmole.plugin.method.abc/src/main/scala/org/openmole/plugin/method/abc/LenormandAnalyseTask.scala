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

import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import fr.iscpif.scalabc._
import org.openmole.core.workflow.task._

object LenormandAnalyseTask {

  def apply(
    lenormand: algorithm.Lenormand with ABC,
    state: Prototype[algorithm.Lenormand#STATE],
    terminated: Prototype[Boolean],
    iteration: Prototype[Int],
    accepted: Prototype[Double])(implicit plugins: PluginSet) = {
    val (_lenormand, _state, _terminated, _iteration, _accepted) = (lenormand, state, terminated, iteration, accepted)

    new TaskBuilder() { builder ⇒
      addInput(state)
      lenormand.priorPrototypes.foreach(t ⇒ addInput(t.toArray))
      lenormand.targetPrototypes.foreach(s ⇒ addInput(s.toArray))
      addOutput(state)
      addOutput(terminated)
      addOutput(iteration)
      addOutput(accepted)

      def toTask = new LenormandAnalyseTask() with Built {
        val lenormand = _lenormand
        val state = _state
        val terminated = _terminated
        val iteration = _iteration
        val accepted = _accepted
      }
    }
  }

}

abstract class LenormandAnalyseTask() extends Task {

  val lenormand: algorithm.Lenormand with ABC
  def state: Prototype[algorithm.Lenormand#STATE]
  def terminated: Prototype[Boolean]
  def iteration: Prototype[Int]
  def accepted: Prototype[Double]

  override def process(context: Context) = {
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
  }
}
