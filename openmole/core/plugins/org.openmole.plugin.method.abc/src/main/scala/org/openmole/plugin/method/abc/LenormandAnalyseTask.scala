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

import org.openmole.core.implementation.task._
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import fr.irstea.scalabc.algorithm.Lenormand
import org.openmole.core.model.task._

object LenormandAnalyseTask {

  def apply(
    name: String,
    lenormand: Lenormand with ABC.ABC,
    state: Prototype[Lenormand#STATE])(implicit plugins: PluginSet) = {
    val (_lenormand, _state) = (lenormand, state)

    new TaskBuilder() { builder ⇒
      addInput(state)
      lenormand.priorPrototypes.foreach(t ⇒ addInput(t.toArray))
      lenormand.targetPrototypes.foreach(s ⇒ addInput(s.toArray))
      addOutput(state)

      def toTask = new LenormandAnalyseTask(name) with Built {
        val lenormand = _lenormand
        val state: Prototype[Lenormand#STATE] = _state
      }
    }
  }

}

abstract class LenormandAnalyseTask(val name: String) extends Task {

  val lenormand: Lenormand with ABC.ABC
  def state: Prototype[Lenormand#STATE]

  override def process(context: Context) = {
    val thetasValue: Seq[Seq[Double]] = lenormand.priorPrototypes.map { p ⇒ context(p.toArray).toSeq }.transpose
    val summaryStatsValue: Seq[Seq[Double]] = lenormand.targetPrototypes.map { p ⇒ context(p.toArray).toSeq }.transpose
    val stateValue: Lenormand#STATE = context(state)
    val nextState = lenormand.analyse(stateValue, thetasValue, summaryStatsValue)
    Context(Variable(state, nextState))
  }
}
