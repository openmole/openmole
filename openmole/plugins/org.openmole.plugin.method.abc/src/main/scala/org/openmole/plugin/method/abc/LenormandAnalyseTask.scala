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
import org.openmole.core.workflow.dsl
import dsl._
import fr.iscpif.scalabc._
import monocle.macros.Lenses
import org.openmole.core.workflow.task._

object LenormandAnalyseTask {

  implicit def isBuilder = TaskBuilder[LenormandAnalyseTask].from(this)

  def apply(
    lenormand:  algorithm.Lenormand with ABC,
    state:      Prototype[algorithm.Lenormand#STATE],
    terminated: Prototype[Boolean],
    iteration:  Prototype[Int],
    accepted:   Prototype[Double]
  ) =
    new LenormandAnalyseTask(
      lenormand,
      state,
      terminated,
      iteration,
      accepted,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    ) set (
      dsl.inputs += state,
      dsl.inputs += (lenormand.priorPrototypes: _*),
      dsl.outputs += (lenormand.targetPrototypes: _*),
      dsl.outputs += (state, terminated, iteration, accepted)
    )

}

@Lenses case class LenormandAnalyseTask(
    lenormand:  algorithm.Lenormand with ABC,
    state:      Prototype[algorithm.Lenormand#STATE],
    terminated: Prototype[Boolean],
    iteration:  Prototype[Int],
    accepted:   Prototype[Double],
    inputs:     PrototypeSet,
    outputs:    PrototypeSet,
    defaults:   DefaultSet,
    name:       Option[String]
) extends Task {

  override def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = {
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
