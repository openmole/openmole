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

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.environment.BatchExecutionJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.cache.AssociativeCache
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.collection.mutable.MultiMap
import scala.ref.WeakReference
import scala.math._
import scala.collection.mutable

object OverSubmissionAgent extends Logger {

  class TimeInt(val value: Int, val time: Long = System.currentTimeMillis)

  implicit class FiniteQueue[A <: { def time: Long }](q: mutable.Queue[A]) {
    def enqueueFinite(elem: A, oldest: Long) = {
      q.enqueue(elem)
      q.dequeueFirst(e ⇒ e.time < oldest)
    }
  }
}

import OverSubmissionAgent._

class OverSubmissionAgent(environment: WeakReference[GliteEnvironment]) extends IUpdatableWithVariableDelay {

  @transient lazy val runningHistory = new mutable.Queue[TimeInt]

  override def delay = Workspace.preferenceAsDuration(GliteEnvironment.OverSubmissionInterval).toMilliSeconds

  override def update: Boolean = {
    try {
      logger.log(FINE, "oversubmission started")

      val env = environment.get match {
        case None ⇒ return false
        case Some(env) ⇒ env
      }

      val registry = env.jobRegistry
      registry.synchronized {
        val jobs = registry.allExecutionJobs
        val stillRunning = jobs.count(_.state == RUNNING)
        val stillReady = jobs.count(_.state == READY)

        runningHistory.enqueueFinite(new TimeInt(stillRunning), Workspace.preferenceAsDuration(GliteEnvironment.RunningHistoryDuration).toMilliSeconds)

        logger.fine("still running " + stillRunning)

        val maxRunning = runningHistory.map(_.value).max * Workspace.preferenceAsDouble(GliteEnvironment.EagerSubmissionThreshold)

        var nbRessub = if (jobs.size > Workspace.preferenceAsInt(GliteEnvironment.OverSubmissionMinNumberOfJob)) {
          val minOversub = Workspace.preferenceAsInt(GliteEnvironment.OverSubmissionMinNumberOfJob)
          if (maxRunning < minOversub) minOversub - jobs.size else maxRunning - (stillRunning + stillReady)
        } else Workspace.preferenceAsInt(GliteEnvironment.OverSubmissionMinNumberOfJob) - jobs.size

        val numberOfSimultaneousExecutionForAJobWhenUnderMinJob = Workspace.preferenceAsInt(GliteEnvironment.OverSubmissionNumberOfJobUnderMin)

        if (nbRessub > 0) {
          // Resubmit nbRessub jobs in a fair manner
          val order = new HashMap[Int, Set[IJob]] with MultiMap[Int, IJob]
          var keys = new TreeSet[Int]

          for (job ← registry.allJobs) {
            val nb = registry.executionJobs(job).size
            if (nb < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
              order.addBinding(nb, job)
              keys += nb
            }
          }

          if (!keys.isEmpty) {
            while (nbRessub > 0 && keys.head < numberOfSimultaneousExecutionForAJobWhenUnderMinJob) {
              val key = keys.head
              val jobs = order(keys.head)
              val job =
                jobs.find(j ⇒ registry.executionJobs(j).isEmpty) match {
                  case Some(j) ⇒ j
                  case None ⇒
                    jobs.find(j ⇒ !registry.executionJobs(j).exists(_.state != SUBMITTED)) match {
                      case Some(j) ⇒ j
                      case None ⇒ jobs.head
                    }
                }

              env.submit(job)

              order.removeBinding(key, job)
              if (jobs.isEmpty) keys -= key

              order.addBinding(key + 1, job)
              keys += (key + 1)
              nbRessub -= 1
            }
          }
        }
      }
    } catch {
      case e: Throwable ⇒ logger.log(SEVERE, "Exception in oversubmission agen", e)
    }
    true
  }

}
