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

package org.openmole.runtime

import java.util.concurrent.Semaphore
import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.tools._
import org.openmole.core.model.job.State._
import scala.collection.immutable.TreeMap
import util.{ Failure, Success, Try }

class ContextSaver(val nbJobs: Int) {

  val allFinished = new Semaphore(0)

  var nbFinished = 0
  var _results = new TreeMap[MoleJobId, (Try[Context], Seq[ITimeStamp[State]])]
  def results = _results

  def save(job: IMoleJob, oldState: State, newState: State) = synchronized {
    newState match {
      case COMPLETED | FAILED ⇒
        job.exception match {
          case None ⇒ _results += job.id -> (Success(job.context), job.timeStamps)
          case Some(t) ⇒ _results += job.id -> (Failure(t), job.timeStamps)
        }
      case _ ⇒
    }

    if (newState.isFinal) {
      nbFinished += 1
      if (nbFinished >= nbJobs) allFinished.release
    }
  }

  def waitAllFinished = {
    allFinished.acquire
    allFinished.release
  }

}
