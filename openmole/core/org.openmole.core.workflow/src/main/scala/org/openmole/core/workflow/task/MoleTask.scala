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

package org.openmole.core.workflow.task

import org.openmole.core.eventdispatcher.{ Event, EventDispatcher, EventListener }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._

import scala.collection.mutable.ListBuffer

object MoleTask {

  def apply(puzzle: Puzzle): MoleTaskBuilder =
    apply(puzzle toMole, puzzle.lasts.head)

  def apply(mole: Mole, last: Capsule) =
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

/**
 * *
 *
 * @param mole the mole executed by this task.
 * @param last the capsule which returns the results
 * @param implicits the implicit values for the inputs
 */
sealed abstract class MoleTask(
    val mole: Mole,
    val last: Capsule,
    val implicits: Iterable[String]) extends Task {

  class ResultGathering extends EventListener[MoleExecution] {
    @volatile var lastContext: Option[Context] = None

    override def triggered(obj: MoleExecution, ev: Event[MoleExecution]) = synchronized {
      ev match {
        case ev: MoleExecution.JobFinished ⇒
          if (ev.capsule == last) lastContext = Some(ev.moleJob.context)
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

    EventDispatcher.listen(execution: MoleExecution, resultGathering, classOf[MoleExecution.JobFinished])
    EventDispatcher.listen(execution: MoleExecution, resultGathering, classOf[MoleExecution.ExceptionRaised])

    execution.start(firstTaskContext)
    execution.waitUntilEnded

    execution.exception.foreach(throw _)

    context + resultGathering.lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
