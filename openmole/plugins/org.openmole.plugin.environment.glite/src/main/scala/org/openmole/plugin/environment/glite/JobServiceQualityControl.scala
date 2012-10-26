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

import org.openmole.core.batch.control._
import java.util.concurrent.atomic._

class JobServiceQualityControl(hysteresis: Int) extends QualityControl(hysteresis) {
  private val _nbSubmitted = new AtomicInteger
  private val _nbRunning = new AtomicInteger
  private val _totalDone = new AtomicLong
  private val _totalSubmitted = new AtomicLong

  def submitted = _nbSubmitted.get
  def runnig = _nbRunning.get
  def totalDone = _totalDone.get
  def totalSubmitted = _totalSubmitted.get

  def incrementSubmitted = {
    _totalSubmitted.incrementAndGet
    _nbSubmitted.incrementAndGet
  }

  def decrementSubmitted = _nbSubmitted.decrementAndGet
  def incrementRunning = _nbRunning.incrementAndGet
  def decrementRunning = _nbRunning.decrementAndGet
  def incrementDone = _totalDone.incrementAndGet
}
