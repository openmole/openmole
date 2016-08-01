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

import monocle.macros.Lenses
import org.openmole.core.event._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.builder
import builder._
import org.openmole.core.exception._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.tool.lock._
import org.openmole.core.workflow.dsl
import dsl._

object MoleTask {

  implicit def isTask = InputOutputBuilder(MoleTask.config)

  def apply(puzzle: Puzzle): MoleTask =
    apply(puzzle toMole, puzzle.lasts.head)

  /**
   * *
   * @param mole the mole executed by this task.
   * @param last the capsule which returns the results
   */
  def apply(mole: Mole, last: Capsule): MoleTask = {
    val mt = new MoleTask(_mole = mole, last = last)

    mt set (
      dsl.inputs += (mole.root.inputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      dsl.outputs += (last.outputs(mole, Sources.empty, Hooks.empty).toSeq: _*),
      isTask.defaults.set(mole.root.task.defaults)
    )
  }

}

@Lenses case class MoleTask(
    _mole:     Mole,
    last:      Capsule,
    implicits: Vector[String]    = Vector.empty,
    config:    InputOutputConfig = InputOutputConfig()
) extends Task {

  def mole = _mole.copy(inputs = inputs)

  protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider): Context = {
    val implicitsValues = implicits.flatMap(i ⇒ context.get(i))

    val execution =
      MoleExecution(
        mole,
        seed = rng().nextLong(),
        implicits = implicitsValues,
        defaultEnvironment = executionContext.localEnvironment,
        tmpDirectory = executionContext.tmpDirectory.newDir("moletask"),
        cleanOnFinish = false
      )

    @volatile var lastContext: Option[Context] = None
    val lastContextLock = new ReentrantLock()

    execution listen {
      case (_, ev: MoleExecution.JobFinished) ⇒
        lastContextLock { if (ev.capsule == last) lastContext = Some(ev.moleJob.context) }
    }

    execution.start(context)
    try execution.waitUntilEnded
    catch {
      case e: ThreadDeath ⇒
        execution.cancel
        throw e
      case e: InterruptedException ⇒
        execution.cancel
        throw e
    }

    context + lastContext.getOrElse(throw new UserBadDataError("Last capsule " + last + " has never been executed."))
  }

}
