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

package org.openmole.core.batch.jobservice

import org.openmole.core.batch.control._
import org.openmole.core.tools.service.Logger
import org.openmole.core.batch.environment._
import org.openmole.core.workflow.execution.ExecutionState._

object JobService extends Logger

import JobService._

trait JobService extends BatchService { js ⇒

  type J

  def submit(serializedJob: SerializedJob)(implicit token: AccessToken): BatchJob = token.synchronized {
    val job = _submit(serializedJob)
    job.state = SUBMITTED
    Log.logger.fine(s"Successful submission: ${job}")
    job
  }

  def state(j: J)(implicit token: AccessToken) = token.synchronized { _state(j) }

  def cancel(j: J)(implicit token: AccessToken) = {
    token.synchronized { _cancel(j) }
    Log.logger.fine(s"Cancelled job: ${j}")
  }

  def purge(j: J)(implicit token: AccessToken) = token.synchronized { _purge(j) }

  protected def _submit(serializedJob: SerializedJob): BatchJob
  protected def _state(j: J): ExecutionState
  protected def _cancel(j: J)
  protected def _purge(j: J)

}
