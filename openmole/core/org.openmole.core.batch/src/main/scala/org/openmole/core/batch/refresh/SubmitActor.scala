/*
 * Copyright (C) 2012 reuillon
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
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.SerializedJob
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.misc.tools.service.Logger
import akka.actor.Actor

object SubmitActor extends Logger

import SubmitActor._

class SubmitActor(jobManager: ActorRef) extends Actor {
  def receive = {
    case Submit(job, sj)=> 
      if(!job.state.isFinal) {
        try {
          val bj = trySubmit(sj, job.environment)
          job.state = SUBMITTED
          jobManager ! Submitted(job, sj, bj)
        } catch {
          case e => 
            jobManager ! Error(job, e)
            jobManager ! Submit(job, sj)
        }
      }
      System.runFinalization
  }
  
  private def trySubmit(serializedJob: SerializedJob, environment: BatchEnvironment) = {
    val (js, token) = environment.selectAJobService
    try js.submit(serializedJob, token)
    finally UsageControl.get(js.description).releaseToken(token)
  }
  
}