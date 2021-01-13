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

package org.openmole.core.workflow.job

import org.openmole.core.workflow.job.State._
import org.openmole.core.workflow.task._
import org.openmole.core.context._

case class RuntimeTask(task: Task, strain: Boolean)

object MoleJob {

  implicit val moleJobOrdering = Ordering.by((_: MoleJob).id)

  type JobFinished = (MoleJobId, Either[Context, Throwable]) ⇒ Unit
  type Canceled = () ⇒ Boolean

  /**
   * Construct from context and UUID
   * @param task
   * @param context context for prototypes and values
   * @param id UUID
   * @param jobFinished
   * @return
   */
  def apply(
    task:            RuntimeTask,
    context:         Context,
    id:              Long,
    jobFinished:     MoleJob.JobFinished,
    subMoleCanceled: Canceled) = {
    val (prototypes, values) = compressContext(context)
    new MoleJob(task, prototypes.toArray, values.toArray, id, jobFinished, subMoleCanceled)
  }

  def compressContext(context: Context) =
    context.variables.toSeq.map {
      case (_, v) ⇒ (v.asInstanceOf[Variable[Any]].prototype, v.value)
    }.unzip

  sealed trait StateChange
  case object Unchanged extends StateChange
  case class Changed(old: State, state: State, context: Context) extends StateChange

  def finish(moleJob: MoleJob, result: Either[Context, Throwable], taskExecutionContext: TaskExecutionContext) = {
    import org.openmole.tool.file._
    taskExecutionContext.taskExecutionDirectory.recursiveDelete
    moleJob.jobFinished(moleJob.id, result)
  }

  class SubMoleCanceled extends Exception

}

import MoleJob._

/**
 * Atomic executable job, wrapping a [[Task]]
 *
 * @param task task to be executed
 * @param prototypes prototypes for the task
 * @param values values of prototypes
 * @param jobFinished what to do when the state is changed
 */
class MoleJob(
  val task:            RuntimeTask,
  prototypes:          Array[Val[Any]],
  values:              Array[Any],
  val id:              MoleJobId,
  val jobFinished:     MoleJob.JobFinished,
  val subMoleCanceled: Canceled) {

  def context: Context =
    Context((prototypes zip values).map { case (p, v) ⇒ Variable(p, v) }: _*)

  def perform(executionContext: TaskExecutionContext): Either[Context, Throwable] =
    if (!subMoleCanceled()) {
      val ctx = context
      try {
        val performResult = task.task.perform(ctx, executionContext)
        Left(if (task.strain) ctx + performResult else performResult)
      }
      catch {
        case t: Throwable ⇒ Right(t)
      }
    }
    else Right(new SubMoleCanceled)

}
