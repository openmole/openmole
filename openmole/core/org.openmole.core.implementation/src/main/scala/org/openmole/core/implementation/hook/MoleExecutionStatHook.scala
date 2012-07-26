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

package org.openmole.core.implementation.hook

import org.openmole.core.model.mole._
import java.sql.Time
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.implementation.job.MoleJob
import org.openmole.core.model.job.State.State
import org.openmole.core.model.job.State._
import org.openmole.core.model.job._

import scala.collection.mutable.ConcurrentMap
import scala.collection.mutable.HashMap
import scala.ref.WeakReference
import collection.JavaConversions._

class MoleExecutionStatHook(val moleExecution: WeakReference[IMoleExecution]) extends MoleExecutionHook {

  @volatile var _jobs: ConcurrentMap[ICapsule, AtomicInteger] = new ConcurrentHashMap[ICapsule, AtomicInteger]

  @volatile var _cpuTime: Long = 0

  def cpuTime = _cpuTime

  def jobs(c: ICapsule) = _jobs.getOrElse(c, new AtomicInteger).get
  def jobs = _jobs.map { case (c, s) ⇒ c -> s.get }

  override def jobStarting(moleJob: IMoleJob, capsule: ICapsule) =
    _jobs.getOrElseUpdate(capsule, new AtomicInteger).incrementAndGet

  override def jobFinished(moleJob: IMoleJob, capsule: ICapsule) =
    _cpuTime += MoleJob.cpuTime(moleJob)

  override def toString = {
    val c = Calendar.getInstance
    c.setTimeInMillis(cpuTime)
    "CPU Time: " + c + "\n" + "Jobs: \n" + jobs.mkString("\n")
  }

}
