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
import org.openmole.misc.tools.service.Logger
import akka.actor.ActorRef
import org.openmole.core.batch.environment.TemporaryErrorException
import org.openmole.core.model.execution.ExecutionState._

object RefreshActor extends Logger

import RefreshActor._

class RefreshActor(jobManager: ActorRef) extends Actor {
  def receive = {
    case Refresh(job, sj, bj, delay)=> 
      if(!job.state.isFinal) {
        try {
          val oldState = job.state
          job.state = bj.updateState
          if(job.state == DONE) {
            jobManager ! GetResult(job, sj, bj.resultPath)
          } else {
            if(!job.state.isFinal) jobManager ! RefreshDelay(job, sj, bj, delay, oldState != job.state)
            else jobManager ! Kill(job)
          }   
        } catch { 
          case e: TemporaryErrorException => logger.log(FINE, "Temporary error durring job update.", e)
          case e => 
            jobManager ! Error(job, e)
            jobManager ! Kill(job)
        }
      }
      System.runFinalization
  }
}
  
