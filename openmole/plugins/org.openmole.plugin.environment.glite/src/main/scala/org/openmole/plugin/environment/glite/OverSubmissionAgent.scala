/*
 * Copyright (C) 2010 reuillon
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
import org.openmole.core.batch.environment.StatisticSample
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.job.IJob
import org.openmole.misc.tools.cache.AssociativeCache
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.updater.IUpdatableWithVariableDelay
import org.openmole.plugin.environment.glite.GliteEnvironment._
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeSet
import scala.collection.mutable.HashSet
import scala.collection.mutable.Set
import scala.collection.mutable.MultiMap
import scala.ref.WeakReference
import scala.math._

object OverSubmissionAgent extends Logger

import OverSubmissionAgent._

class OverSubmissionAgent(
    environment: WeakReference[GliteEnvironment]) extends IUpdatableWithVariableDelay {

  def this(environment: GliteEnvironment) = this(new WeakReference(environment))

  override def delay = Workspace.preferenceAsDurationInMs(OverSubmissionInterval)

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
        // registry.allExecutionJobs.groupBy(ejob => (ejob.job.executionId, new StatisticKey(ejob.job))).
        //                         filter( elt => {elt._1 != null && elt._2.size > 0} )
        //Logger.getLogger(classOf[OverSubmissionAgent].getName).log(Level.FINE,"size " + toProceed.size + " all " + registry.allExecutionJobs)

        val now = System.currentTimeMillis
        val stillRunning = jobs.filter(_.state == RUNNING)
        val stillReady = jobs.filter(_.state == READY)

        logger.fine("still running " + stillRunning.size)

        val stillRunningSamples = jobs.view.flatMap { _.batchJob }.filter(_.state == RUNNING).map { j ⇒ new StatisticSample(j.timeStamp(SUBMITTED), j.timeStamp(RUNNING), now) }

        val samples = (env.statistics ++ stillRunningSamples).toArray

        logger.fine("still running samples " + stillRunningSamples.size + " samples size " + samples.size)

        var nbRessub = if (!samples.isEmpty && jobs.size > Workspace.preferenceAsInt(OverSubmissionMinNumberOfJob)) {
          val windowSize = (jobs.size * Workspace.preferenceAsDouble(OverSubmissionSamplingWindowFactor)).toInt
          val windowStart = if (samples.size - 1 > windowSize) samples.size - 1 - windowSize else 0

          val nbSamples = Workspace.preferenceAsInt(OverSubmissionNbSampling)
          val interval = (samples.last.done - samples(windowStart).submitted) / (nbSamples)

          //Logger.getLogger(classOf[OverSubmissionAgent].getName).log(Level.FINE,"interval " + interval)

          val maxNbRunning =
            (for (date ← (samples(windowStart).submitted) until (samples.last.done, interval)) yield samples.count(s ⇒ s.running <= date && s.done >= date)).max

          logger.fine("max running " + maxNbRunning)

          val minOversub = Workspace.preferenceAsInt(OverSubmissionMinNumberOfJob)
          if (maxNbRunning < minOversub) minOversub - jobs.size else maxNbRunning - (stillRunning.size + stillReady.size)
        } else Workspace.preferenceAsInt(OverSubmissionMinNumberOfJob) - jobs.size

        logger.fine("NbRessub " + nbRessub)
        val numberOfSimultaneousExecutionForAJobWhenUnderMinJob = Workspace.preferenceAsInt(OverSubmissionNumberOfJobUnderMin)

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
      case e ⇒ logger.log(SEVERE, "Exception in oversubmission agen", e)
    }
    true
  }

}
