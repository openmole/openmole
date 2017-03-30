/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.batch.refresh

import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.environment.BatchEnvironment
import org.openmole.tool.logger.Logger

object SubmitActor {

  def receive(submit: Submit)(implicit services: BatchEnvironment.Services) = {
    import services._

    val Submit(job, sj) = submit
    if (!job.state.isFinal) {
      try job.trySelectJobService match {
        case Some((js, token)) ⇒
          val bj =
            try js.submit(sj)(token)
            finally js.releaseToken(token)
          job.state = SUBMITTED
          JobManager ! Submitted(job, sj, bj)
        case None ⇒ JobManager ! Delay(submit, BatchEnvironment.getTokenInterval)
      }
      catch {
        case e: Throwable ⇒
          JobManager ! Error(job, e)
          JobManager ! Submit(job, sj)
      }
    }
  }

}