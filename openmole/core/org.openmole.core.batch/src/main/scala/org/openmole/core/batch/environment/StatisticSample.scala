/*
 * Copyright (C) 2011 reuillon
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

import org.openmole.core.model.execution.ExecutionState._

object StatisticSample {
  implicit val orderingByDone = new Ordering[StatisticSample] {
    def compare (x: StatisticSample, y: StatisticSample): Int = (x.done - y.done).toInt
  }
}

class StatisticSample(val submitted: Long, val running: Long, val done: Long) {
  def this(job: BatchJob) = this(job.timeStamp(SUBMITTED), job.timeStamp(RUNNING), job.timeStamp(DONE))
}