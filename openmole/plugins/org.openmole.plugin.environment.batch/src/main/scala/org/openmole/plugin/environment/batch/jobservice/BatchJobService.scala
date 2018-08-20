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

  def submit(js: JS, serializedJob: SerializedJob): J
  def state(js: JS, j: J): ExecutionState
  def delete(js: JS, j: J): Unit
  def stdOutErr(js: JS, j: J): (String, String)

  def usageControl(js: JS): UsageControl
}

object BatchJobService extends JavaLogger {

  def tryStdOutErr(batchJob: BatchJobControl, token: AccessToken) = util.Try(batchJob.stdOutErr(token))

  def submit[JS](jobService: JS, serializedJob: SerializedJob, resultPath: AccessToken ⇒ String)(implicit token: AccessToken, jobServiceInterface: JobServiceInterface[JS]): BatchJobControl = token.access {

    def updateState(job: jobServiceInterface.J)(token: AccessToken): ExecutionState = token.access { jobServiceInterface.state(jobService, job) }
    def delete(job: jobServiceInterface.J)(token: AccessToken) = token.access { jobServiceInterface.delete(jobService, job) }
    def stdOutErr(job: jobServiceInterface.J)(token: AccessToken) = token.access { jobServiceInterface.stdOutErr(jobService, job) }

    val job = jobServiceInterface.submit(jobService, serializedJob)
    BatchJobService.Log.logger.fine(s"Successful submission: ${job}")

    BatchJobControl(
      updateState(job),
      delete(job),
      stdOutErr(job),
      resultPath,
      jobServiceInterface.usageControl(jobService)
    )
  }

}

object BatchJobControl {

  def apply(
    updateState:  AccessToken ⇒ ExecutionState,
    delete:       AccessToken ⇒ Unit,
    stdOutErr:    AccessToken ⇒ (String, String),
    resultPath:   AccessToken ⇒ String,
    usageControl: UsageControl): BatchJobControl = new BatchJobControl(
    updateState,
    delete,
    stdOutErr,
    resultPath,
    usageControl)

}

class BatchJobControl(
  val updateState:  AccessToken ⇒ ExecutionState,
  val delete:       AccessToken ⇒ Unit,
  val stdOutErr:    AccessToken ⇒ (String, String),
  val resultPath:   AccessToken ⇒ String,
  val usageControl: UsageControl)
