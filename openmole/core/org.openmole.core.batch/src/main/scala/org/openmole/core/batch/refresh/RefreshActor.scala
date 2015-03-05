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

import akka.actor.Actor
import akka.actor.ActorRef
import org.openmole.core.tools.service.Logger
import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.core.batch.environment.{ ResubmitException, BatchEnvironment }
import org.openmole.core.workspace.Workspace

object RefreshActor extends Logger

import RefreshActor.Log._

class RefreshActor(jobManager: ActorRef) extends Actor {

  def receive = withRunFinalization {
    case Refresh(job, sj, bj, delay, updateErrorsInARow) ⇒
      if (!job.state.isFinal) {
        try bj.jobService.tryWithToken {
          case Some(t) ⇒
            val oldState = job.state
            job.state = bj.updateState(t)
            if (job.state == DONE) jobManager ! GetResult(job, sj, bj.resultPath)
            else if (!job.state.isFinal) {
              val newDelay =
                if (oldState == job.state) (delay + job.environment.incrementUpdateInterval) min job.environment.maxUpdateInterval
                else job.environment.minUpdateInterval
              jobManager ! Delay(Refresh(job, sj, bj, newDelay, 0), newDelay)
            }
            else jobManager ! Kill(job)
          case None ⇒ jobManager ! Delay(Refresh(job, sj, bj, delay, updateErrorsInARow), delay)
        } catch {
          case _: ResubmitException ⇒
            jobManager ! Resubmit(job, sj.storage)
          case e: Throwable ⇒
            if (updateErrorsInARow >= Workspace.preferenceAsInt(MaxUpdateErrorsInARow)) {
              jobManager ! Error(job, e)
              jobManager ! Kill(job)
            }
            else {
              logger.log(FINE, s"${updateErrorsInARow + 1} errors in a row during job refresh", e)
              jobManager ! Delay(Refresh(job, sj, bj, delay, updateErrorsInARow + 1), delay)
            }
        }
      }
  }
}

