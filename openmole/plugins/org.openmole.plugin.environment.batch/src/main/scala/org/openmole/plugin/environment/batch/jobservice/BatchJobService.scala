/*
 * Copyright (C) 2010 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.environment.batch.jobservice

import org.openmole.core.event.EventDispatcher
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.logger.JavaLogger

import scala.concurrent.stm._

trait JobServiceInterface[JS] {
  type J

  def submit(js: JS, serializedJob: SerializedJob): BatchJob[J]
  def state(js: JS, j: J): ExecutionState
  def delete(js: JS, j: J): Unit
  def stdOutErr(js: JS, j: J): (String, String)
}

object BatchJobService extends JavaLogger {
  def apply[JS](js: JS, concurrency: Int)(implicit _jobServiceInterface: JobServiceInterface[JS], eventDispatcher: EventDispatcher) =
    new BatchJobService[JS](js, UsageControl(concurrency))

}

trait BatchJobControl {
  def updateState(implicit token: AccessToken): ExecutionState
  def delete(implicit token: AccessToken): Unit
  def stdOutErr(implicit token: AccessToken): (String, String)
  def state: ExecutionState
  def usageControl: UsageControl
  def resultPath: String
}

class BatchJobService[JS](
  js:               JS,
  val usageControl: UsageControl
)(implicit val jsInterface: JobServiceInterface[JS], eventDispatcher: EventDispatcher) { bjs ⇒

  type BJ = BatchJob[jsInterface.J]

  case class BatchJobControlImplementation(bj: BJ, usageControl: UsageControl) extends BatchJobControl {
    def updateState(implicit token: AccessToken) = bjs.updateState(bj)
    def delete(implicit token: AccessToken) = bjs.delete(bj)
    def stdOutErr(implicit token: AccessToken) = jsInterface.stdOutErr(js, bj.id)
    def state = bjs.state(bj)
    def resultPath = bj.resultPath
  }

  lazy val states = TMap[BJ, ExecutionState]()

  def submit(serializedJob: SerializedJob)(implicit token: AccessToken): BatchJobControl = token.access {
    val job = jsInterface.submit(js, serializedJob)
    setState(job, SUBMITTED)
    BatchJobService.Log.logger.fine(s"Successful submission: ${job}")
    BatchJobControlImplementation(job, usageControl)
  }

  protected def getState(job: BJ) = states.single.get(job)

  protected def setState(job: BJ, state: ExecutionState) = {
    val event =
      atomic { implicit ctx ⇒
        val oldState = bjs.state(job)
        if (oldState < state) {
          states.put(job, state)
          Some(new BatchJob.StateChanged(oldState, state))
        }
        else None
      }

    event.foreach(ev ⇒ eventDispatcher.trigger(job, ev))
  }

  def state(job: BJ) = getState(job).getOrElse(READY)
  def hasBeenSubmitted(job: BJ): Boolean = state(job).compareTo(SUBMITTED) >= 0

  def updateState(job: BJ)(implicit token: AccessToken): ExecutionState = {
    val remoteState = token.access { jsInterface.state(js, job.id) }
    setState(job, remoteState)
    remoteState
  }

  def delete(job: BJ)(implicit token: AccessToken) = {
    val exists = states.single.remove(job)
    if (exists.isDefined) token.access { jsInterface.delete(js, job.id) }
  }

}

