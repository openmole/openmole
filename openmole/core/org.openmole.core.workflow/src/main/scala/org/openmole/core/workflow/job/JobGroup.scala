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

package org.openmole.core.workflow.job

import org.openmole.core.workflow.mole.*

/**
 * A computation job to be executed
 */
sealed trait JobGroup

object JobGroup:

  def apply(moleExecution: MoleExecution, moleJobs: IArray[Job]): JobGroup =
    if moleJobs.size == 1
    then JobGroup(moleExecution, moleJobs.head)
    else MultiJobGroup(moleExecution, moleJobs)

  def apply(moleExecution: MoleExecution, moleJob: Job): JobGroup = SingleJobGroup(moleExecution, moleJob)

  case class SingleJobGroup(moleExecution: MoleExecution, moleJob: Job) extends JobGroup
  case class MultiJobGroup(moleExecution: MoleExecution, moleJobs: IArray[Job]) extends JobGroup

  /**
   * the [[Job]] in this job
   *
   * @return
   */
  def moleJobs(job: JobGroup): IArray[Job] =
    job match
      case sj: SingleJobGroup => IArray(sj.moleJob)
      case mj: MultiJobGroup  => mj.moleJobs

  def moleJobsValue(job: JobGroup): Job | IArray[Job] =
    job match
      case sj: SingleJobGroup => sj.moleJob
      case mj: MultiJobGroup => mj.moleJobs


  /**
   * Execution of the job
   * @return
   */
  def moleExecution(job: JobGroup): MoleExecution =
    job match
      case sj: SingleJobGroup => sj.moleExecution
      case mj: MultiJobGroup  => mj.moleExecution

  given Ordering[JobGroup] = Ordering.by[JobGroup, Iterable[Job]](moleJobs)
