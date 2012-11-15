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
import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.environment.BatchEnvironment
import akka.actor.ActorRef

object KillerActor extends Logger

import org.openmole.core.model.execution.ExecutionState._
import KillerActor._

class KillerActor(jobManager: ActorRef) extends Actor {
  def receive = {
    case KillBatchJob(bj, nbTry) ⇒
      try bj.jobService.tryWithToken {
        case Some(t) ⇒ bj.kill(t)
        case None ⇒
          if (nbTry < Workspace.preferenceAsInt(BatchEnvironment.NoTokenForSerivceRetry))
            jobManager ! Delay(KillBatchJob(bj, nbTry + 1), Workspace.preferenceAsDuration(BatchEnvironment.NoTokenForSerivceRetryInterval).toMilliSeconds)
      } catch {
        case e: Throwable ⇒ logger.log(FINE, "Could not kill job.", e)
      }
      System.runFinalization
  }
}
