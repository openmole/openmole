/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.task

import org.openmole.misc.tools.service.Random
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.exception._
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.mole.Capsule._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._

object MoleTask {

  def apply(name: String, puzzle: Puzzle)(implicit plugins: PluginSet): TaskBuilder =
    apply(name, new Mole(puzzle.first), puzzle.lasts.head)

  def apply(name: String, mole: IMole, last: ICapsule)(implicit plugins: PluginSet) = {
    new TaskBuilder { builder ⇒
      def toTask = new MoleTask(name, mole, last) {
        val inputs = builder.inputs + mole.root.inputs
        val outputs = builder.outputs + last.outputs
        val parameters = builder.parameters
      }
    }
  }

}

sealed abstract class MoleTask(
    val name: String,
    val mole: IMole,
    val last: ICapsule)(implicit val plugins: PluginSet) extends Task with IMoleTask {

  class ResultGathering extends EventListener[IMoleExecution] {
    var lastContext: Option[Context] = None
    var exceptions: List[Throwable] = List.empty

    override def triggered(obj: IMoleExecution, ev: Event[IMoleExecution]) = synchronized {
      ev match {
        case ev: IMoleExecution.JobInCapsuleFinished ⇒
          if (ev.capsule == last) lastContext = Some(ev.moleJob.context)
        case ev: IMoleExecution.ExceptionRaised ⇒
          exceptions ::= ev.exception
        case _ ⇒
      }
    }
  }

  override protected def process(context: Context) = {
    val firstTaskContext = inputs.foldLeft(List.empty[Variable[_]]) {
      (acc, input) ⇒
        if (!(input.mode is optional) || ((input.mode is optional) && context.contains(input.prototype)))
          context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found.")) :: acc
        else acc
    }.toContext

    val execution = new MoleExecution(mole, rng = Random.newRNG(context.valueOrException(Task.openMOLESeed)))
    val resultGathering = new ResultGathering

    EventDispatcher.listen(execution: IMoleExecution, resultGathering, classOf[IMoleExecution.JobInCapsuleFinished])
    EventDispatcher.listen(execution: IMoleExecution, resultGathering, classOf[IMoleExecution.ExceptionRaised])

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    if (!resultGathering.exceptions.isEmpty) throw new MultipleException(resultGathering.exceptions.reverse)

    context + resultGathering.lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
