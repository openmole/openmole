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

import java.util.concurrent.locks.ReentrantLock

import org.openmole.core.event._
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.builder.TaskBuilder
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.tool.lock._

import scala.collection.mutable.ListBuffer

object MoleTask {

  def apply(puzzle: Puzzle): MoleTaskBuilder =
    apply(puzzle toMole, puzzle.lasts.head)

  def apply(mole: Mole, last: Capsule) =
    new MoleTaskBuilder { builder ⇒
      addInput(mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq: _*)
      addOutput(last.outputs(mole, Sources.empty, Hooks.empty).toSeq: _*)
      def toTask = new MoleTask(mole.copy(inputs = builder.inputs), last, implicits) with builder.Built
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

  override def perform(context: Context, localEnvironment: LocalEnvironment)(rng: RandomProvider): Context = perform(context, executeMole(_, localEnvironment, rng))

  def executeMole(context: Context, localEnvironment: LocalEnvironment, rng: RandomProvider) = {
    val implicitsValues = implicits.flatMap(i ⇒ context.get(i))

    val execution = MoleExecution(mole, seed = rng().nextLong(), implicits = implicitsValues, defaultEnvironment = localEnvironment)

    @volatile var lastContext: Option[Context] = None
    val lastContextLock = new ReentrantLock()

    execution listen {
      case (_, ev: MoleExecution.JobFinished) ⇒
        lastContextLock { if (ev.capsule == last) lastContext = Some(ev.moleJob.context) }
    }

    execution.start(context)
    try execution.waitUntilEnded
    catch {
      case e: InterruptedException ⇒
        execution.cancel
        throw e
    }

    context + lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

  override def process(context: Context)(implicit rng: RandomProvider) = throw new InternalProcessingError("This method should never be called")

}
