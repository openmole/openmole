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

import org.openmole.core.workflow.task._
import org.openmole.core.context._

case class RuntimeTask(task: Task, strain: Boolean)

object Job:

  given Ordering[Job] = Ordering.by((_: Job).id)

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
    jobFinished:     JobFinished,
    subMoleCanceled: Canceled): Job = apply(task, context, id, CallBack(jobFinished, subMoleCanceled))

  def apply(
    task:     RuntimeTask,
    context:  Context,
    id:       Long,
    callBack: CallBack) =
    new Job(task, CompactedContext.compact(context), id, callBack)

  sealed trait StateChange
  case object Unchanged extends StateChange
  case class Changed(old: State, state: State, context: Context) extends StateChange

  def finish(moleJob: Job, result: Either[Context, Throwable]) = moleJob.callBack.jobFinished(moleJob.id, result)

  class SubMoleCanceled extends Exception

  object CallBack:
    def apply(jobFinished: JobFinished, canceled: Canceled) = Instance(jobFinished, canceled)

    case class Instance(_jobFinished: JobFinished, _canceled: Canceled) extends CallBack:
      def subMoleCanceled() = _canceled()
      def jobFinished(job: JobId, result: Either[Context, Throwable]) = _jobFinished(job, result)

  trait CallBack:
    def jobFinished(id: JobId, result: Either[Context, Throwable]): Unit
    def subMoleCanceled(): Boolean

  type JobFinished = (JobId, Either[Context, Throwable]) ⇒ Unit
  type Canceled = () ⇒ Boolean
  
  //def compact(job: Job) = Array(job.task, job.compressedContext, job.id, job.callBack)

import Job._

/**
 * Atomic executable job, wrapping a [[Task]]
 *
 * @param task task to be executed
 * @param prototypes prototypes for the task
 * @param values values of prototypes
 * @param jobFinished what to do when the state is changed
 */
class Job(
  val task:          RuntimeTask,
  private val compressedContext: CompactedContext,
  val id:            JobId,
  val callBack:      CallBack):

  def context: Context = CompactedContext.expand(compressedContext)

  def perform(executionContext: TaskExecutionContext): Either[Context, Throwable] =
    if !callBack.subMoleCanceled()
    then
      val ctx = context
      try
        val performResult = Task.perform(task.task, ctx, executionContext)
        Left(if (task.strain) ctx + performResult else performResult)
      catch
        case t: Throwable ⇒ Right(t)
    else Right(new SubMoleCanceled)

