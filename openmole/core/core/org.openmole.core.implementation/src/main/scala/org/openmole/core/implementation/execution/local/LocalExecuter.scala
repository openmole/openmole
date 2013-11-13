/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.execution.local

import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution._
import org.openmole.core.model.execution.Environment._
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State
import org.openmole.core.model.task.IMoleTask
import org.openmole.core.implementation.job.MoleJob._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.{ LocalHostName, Logger }
import scala.collection.JavaConversions._
import ref.WeakReference
import org.openmole.core.implementation.mole.StrainerCapsule

object LocalExecuter extends Logger

class LocalExecuter(environment: WeakReference[LocalEnvironment]) extends Runnable {

  import LocalExecuter._

  var stop: Boolean = false

  override def run = {

    while (!stop) {
      environment.get match {
        case Some(environment) ⇒
          def jobGoneIdle() {
            environment.addExecuters(1)
            stop = true
          }

          val executionJob = environment.takeNextjob

          val beginTime = System.currentTimeMillis

          try {
            executionJob.state = ExecutionState.RUNNING

            for (moleJob ← executionJob.moleJobs) {
              if (moleJob.state != State.CANCELED) {
                moleJob.task match {
                  case _: IMoleTask ⇒ jobGoneIdle()
                  case t: StrainerCapsule.StrainerTaskDecorator ⇒
                    if (classOf[IMoleTask].isAssignableFrom(t.task.getClass)) jobGoneIdle()
                  case _ ⇒
                }
                moleJob.perform
                moleJob.exception match {
                  case Some(e) ⇒ EventDispatcher.trigger(environment: Environment, MoleJobExceptionRaised(executionJob, e, SEVERE, moleJob))
                  case _       ⇒
                }
              }
            }
            executionJob.state = ExecutionState.DONE

            val endTime = System.currentTimeMillis

            val log = RuntimeLog(beginTime, beginTime, endTime, endTime, LocalHostName.localHostName)
            EventDispatcher.trigger(environment: Environment, Environment.JobCompleted(executionJob, log))

          }
          catch {
            case e: InterruptedException ⇒
              if (!stop) {
                logger.log(WARNING, "Interrupted despite stop is false", e)
                EventDispatcher.trigger(environment: Environment, ExceptionRaised(executionJob, e, SEVERE))
              }
            case e: Throwable ⇒
              logger.log(SEVERE, "Error in execution", e)
              EventDispatcher.trigger(environment: Environment, ExceptionRaised(executionJob, e, SEVERE))
          }
          finally executionJob.state = ExecutionState.KILLED
        case None ⇒ stop = true
      }
    }
  }

}
