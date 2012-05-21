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

package org.openmole.core.batch.control

import java.util.concurrent.atomic._

class JobServiceQualityControl(hysteresis: Int) extends QualityControl(hysteresis) {
  private val _nbSubmitted = new AtomicInteger
  private val _nbRunning = new AtomicInteger
  private val _nbDone = new AtomicLong

  def submitted = _nbSubmitted.get
  def runnig = _nbRunning.get
  def done = _nbDone.get

  def incrementSubmitted = _nbSubmitted.incrementAndGet
  def decrementSubmitted = _nbSubmitted.decrementAndGet
  def incrementRunning = _nbRunning.incrementAndGet
  def decrementRunning = _nbRunning.decrementAndGet
  def incrementDone = _nbDone.incrementAndGet
}
