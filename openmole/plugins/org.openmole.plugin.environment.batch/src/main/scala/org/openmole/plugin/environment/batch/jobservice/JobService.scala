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

import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.control._
import org.openmole.plugin.environment.batch.environment._
import org.openmole.tool.logger.Logger

object JobService extends Logger

import org.openmole.plugin.environment.batch.jobservice.JobService._

trait JobService extends BatchService { js ⇒

  type J

  def submit(serializedJob: SerializedJob)(implicit token: AccessToken): BatchJob = token.access {
    val job = _submit(serializedJob)
    job.state = SUBMITTED
    Log.logger.fine(s"Successful submission: ${job}")
    job
  }

  def state(j: J)(implicit token: AccessToken) = token.access { _state(j) }

  def delete(j: J)(implicit token: AccessToken) = {
    token.synchronized { _delete(j) }
    Log.logger.fine(s"Cancelled job: ${j}")
  }

  protected def _submit(serializedJob: SerializedJob): BatchJob
  protected def _state(j: J): ExecutionState
  protected def _delete(j: J)

}
