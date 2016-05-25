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

package org.openmole.core.batch.refresh

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.workspace.Workspace
import org.openmole.tool.logger.Logger

object KillerActor extends Logger

import KillerActor.Log._

class KillerActor(jobManager: JobManager) {
  def receive(msg: KillBatchJob) = {
    val KillBatchJob(bj) = msg
    try bj.jobService.tryWithToken {
      case Some(t) ⇒ bj.kill(t)
      case None ⇒
        jobManager ! Delay(msg, BatchEnvironment.getTokenInterval)
    } catch {
      case e: Throwable ⇒ logger.log(FINE, "Could not kill job.", e)
    }
  }
}
