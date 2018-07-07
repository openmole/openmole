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

import org.openmole.core.workflow.mole.MoleExecution

sealed trait Job {
  def moleJobs: Iterable[MoleJob]
  def finished: Boolean = moleJobs.forall { _.finished }
  def moleExecution: MoleExecution
}

object Job {

  def apply(moleExecution: MoleExecution, moleJobs: Iterable[MoleJob]): Job =
    (moleJobs.size == 1) match {
      case true  ⇒ Job(moleExecution, moleJobs.head)
      case false ⇒ new MultiJob(moleExecution, moleJobs.toArray)
    }

  def apply(moleExecution: MoleExecution, moleJob: MoleJob): Job =
    new SingleJob(moleExecution, moleJob)

  class SingleJob(val moleExecution: MoleExecution, val moleJob: MoleJob) extends Job {
    def moleJobs = Vector(moleJob)
  }

  class MultiJob(val moleExecution: MoleExecution, _moleJobs: Array[MoleJob]) extends Job {
    def moleJobs = _moleJobs.toIterable
  }

  implicit def ordering = Ordering.by[Job, Iterable[MoleJob]](_.moleJobs)

}