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
import org.openmole.misc.tools.service.Logger
import akka.actor.ActorRef
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.batch.environment.BatchEnvironment._
import org.openmole.core.batch.environment.BatchEnvironment

object RefreshActor extends Logger

import RefreshActor._

class RefreshActor(jobManager: ActorRef, environment: BatchEnvironment) extends Actor {
  def receive = {
    case Refresh(job, sj, bj, delay) ⇒
      if (!job.state.isFinal) {
        try {
          bj.jobService.tryGetToken match {
            case Some(token) ⇒
              try {
                val oldState = job.state
                job.state = bj.updateState(token)
                if (job.state == DONE) jobManager ! GetResult(job, sj, bj.resultPath)
                else if (!job.state.isFinal) {
                  val newDelay =
                    if (oldState == job.state) math.min(delay + environment.incrementUpdateInterval, environment.maxUpdateInterval)
                    else environment.minUpdateInterval
                  jobManager ! RefreshDelay(job, sj, bj, newDelay)
                } else jobManager ! Kill(job)
              } finally bj.jobService.releaseToken(token)
            case None ⇒ jobManager ! RefreshDelay(job, sj, bj, delay)
          }
        } catch {
          case e: Throwable ⇒
            jobManager ! Error(job, e)
            jobManager ! Kill(job)
        }
      }
      System.runFinalization
  }
}

