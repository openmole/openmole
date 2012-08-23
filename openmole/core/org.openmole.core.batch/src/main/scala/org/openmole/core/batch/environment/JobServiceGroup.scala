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

package org.openmole.core.batch.environment

import org.openmole.misc.tools.service.Random
import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.JobServiceControl
import org.openmole.core.batch.control.UsageControl
import ServiceGroup._
import scala.annotation.tailrec

import scala.concurrent.stm._

class JobServiceGroup(val environment: BatchEnvironment, resources: Iterable[JobService]) extends ServiceGroup with Iterable[JobService] {

  override def iterator = resources.iterator

  def selectAService: (JobService, AccessToken) = {
    if (resources.size == 1) {
      val r = resources.head
      return (r, UsageControl.get(r.description).waitAToken)
    }

    def fitness =
      resources.flatMap {
        cur ⇒
          UsageControl.get(cur.description).tryGetToken match {
            case None ⇒ None
            case Some(token) ⇒
              val q = JobServiceControl.qualityControl(cur.description).get

              val nbSubmitted = q.submitted
              val fitness = orMin(
                if (q.submitted > 0)
                  math.pow((q.runnig.toDouble / q.submitted) * q.successRate * (q.totalDone / q.totalSubmitted), 2)
                else math.pow(q.successRate, 2))
              Some((cur, token, fitness))
          }
      }

    @tailrec def selected(value: Double, storages: List[(JobService, AccessToken, Double)]): Option[(JobService, AccessToken)] =
      storages.headOption match {
        case Some((js, token, fitness)) ⇒
          if (value <= fitness) Some((js, token))
          else selected(value - fitness, storages.tail)
        case None ⇒ None
      }

    atomic { implicit txn ⇒
      val notLoaded = fitness
      selected(Random.default.nextDouble * notLoaded.map { case (_, _, fitness) ⇒ fitness }.sum, notLoaded.toList) match {
        case Some((jobService, token)) ⇒
          for {
            (s, t, _) ← notLoaded
            if (s.description != jobService.description)
          } UsageControl.get(s.description).releaseToken(t)
          jobService -> token
        case None ⇒ retry
      }
    }
  }

}
