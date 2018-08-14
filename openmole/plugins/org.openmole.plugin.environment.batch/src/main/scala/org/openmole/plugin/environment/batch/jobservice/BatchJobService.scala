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

case class BatchJob[J](id: J, resultPath: String)

object BatchJobService extends JavaLogger {

  def apply[JS](js: JS, concurrency: Int)(implicit jobServiceInterface: JobServiceInterface[JS], eventDispatcher: EventDispatcher) =
    new BatchJobService[JS](js, UsageControl(concurrency), jobServiceInterface)

  def tryStdOutErr(batchJob: BatchJobControl, token: AccessToken) = util.Try(batchJob.stdOutErr(token))

  def submit[JS](batchJobService: BatchJobService[JS], serializedJob: SerializedJob)(implicit token: AccessToken): BatchJobControl = token.access {
    import batchJobService._

    type BJ = BatchJob[jsInterface.J]

    def updateState(job: BJ)(token: AccessToken): ExecutionState = token.access { jsInterface.state(js, job.id) }
    def delete(job: BJ)(token: AccessToken) = token.access { jsInterface.delete(js, job.id) }
    def stdOutErr(job: BJ)(token: AccessToken) = token.access { jsInterface.stdOutErr(js, job.id) }

    val job: BJ = jsInterface.submit(js, serializedJob)
    BatchJobService.Log.logger.fine(s"Successful submission: ${job}")

    BatchJobControl(
      updateState(job),
      delete(job),
      stdOutErr(job),
      usageControl,
      job.resultPath
    )
  }

}

class BatchJobService[JS](
  val js:           JS,
  val usageControl: UsageControl,
  val jsInterface:  JobServiceInterface[JS])

object BatchJobControl {

  def apply(
    updateState:  AccessToken ⇒ ExecutionState,
    delete:       AccessToken ⇒ Unit,
    stdOutErr:    AccessToken ⇒ (String, String),
    usageControl: UsageControl,
    resultPath:   String): BatchJobControl = new BatchJobControl(
    updateState,
    delete,
    stdOutErr,
    usageControl,
    resultPath)

}

class BatchJobControl(
  val updateState:  AccessToken ⇒ ExecutionState,
  val delete:       AccessToken ⇒ Unit,
  val stdOutErr:    AccessToken ⇒ (String, String),
  val usageControl: UsageControl,
  val resultPath:   String)
