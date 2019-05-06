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

import org.openmole.core.workflow.mole._

/**
 * A computation job to be executed
 */
sealed trait Job

object Job {

  def apply(moleExecution: MoleExecution, moleJobs: Iterable[MoleJob]): Job =
    (moleJobs.size == 1) match {
      case true  ⇒ Job(moleExecution, moleJobs.head)
      case false ⇒ MultiJob(moleExecution, moleJobs.toArray)
    }

  def apply(moleExecution: MoleExecution, moleJob: MoleJob): Job = SingleJob(moleExecution, moleJob)

  case class SingleJob(moleExecution: MoleExecution, moleJob: MoleJob) extends Job
  case class MultiJob(moleExecution: MoleExecution, moleJobs: Array[MoleJob]) extends Job

  /**
   * the [[MoleJob]] in this job
   * @return
   */
  def moleJobs(job: Job) =
    job match {
      case sj: SingleJob ⇒ Vector(sj.moleJob)
      case mj: MultiJob  ⇒ mj.moleJobs.toIterable
    }

  /**
   * the Job is finished if all mole jobs are finished
   * @return
   */
  def finished(job: Job): Boolean = moleJobs(job).forall { _.finished }

  /**
   * Execution of the job
   * @return
   */
  def moleExecution(job: Job): MoleExecution =
    job match {
      case sj: SingleJob ⇒ sj.moleExecution
      case mj: MultiJob  ⇒ mj.moleExecution
    }

  implicit def ordering = Ordering.by[Job, Iterable[MoleJob]](moleJobs)

}