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

import org.openmole.core.implementation.builder.TaskBuilder
import org.openmole.misc.eventdispatcher._
import org.openmole.misc.exception._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.mole._
import org.openmole.core.model.mole._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import scala.collection.mutable.ListBuffer

object MoleTask {

  def apply(puzzle: Puzzle)(implicit plugins: PluginSet): MoleTaskBuilder =
    apply(puzzle toMole, puzzle.lasts.head)

  def apply(mole: IMole, last: ICapsule)(implicit plugins: PluginSet) =
    new MoleTaskBuilder { builder ⇒
      addInput(mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq: _*)
      addOutput(last.outputs(mole, Sources.empty, Hooks.empty).toSeq: _*)
      def toTask = new MoleTask(mole, last, implicits) with builder.Built
    }

  trait MoleTaskBuilder extends TaskBuilder { builder ⇒
    val implicits = ListBuffer[String]()
    def addImplicit(p: String) = implicits += p
  }

}

sealed abstract class MoleTask(
    val mole: IMole,
    val last: ICapsule,
    val implicits: Iterable[String]) extends Task with IMoleTask {

  class ResultGathering extends EventListener[IMoleExecution] {
    @volatile var lastContext: Option[Context] = None
    @volatile var exceptions: List[Throwable] = List.empty

    override def triggered(obj: IMoleExecution, ev: Event[IMoleExecution]) = synchronized {
      ev match {
        case ev: IMoleExecution.JobFinished ⇒
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
        if (!(input.mode is Optional) || ((input.mode is Optional) && context.contains(input.prototype)))
          context.variable(input.prototype).getOrElse(throw new InternalProcessingError("Bug: variable not found.")) :: acc
        else acc
    }.toContext

    val implicitsValues = implicits.flatMap(i ⇒ context.get(i))

    val execution = MoleExecution(mole, seed = context(Task.openMOLESeed), implicits = implicitsValues)
    val resultGathering = new ResultGathering

    EventDispatcher.listen(execution: IMoleExecution, resultGathering, classOf[IMoleExecution.JobFinished])
    EventDispatcher.listen(execution: IMoleExecution, resultGathering, classOf[IMoleExecution.ExceptionRaised])

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    if (!resultGathering.exceptions.isEmpty) throw new MultipleException(resultGathering.exceptions.reverse)

    context + resultGathering.lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
