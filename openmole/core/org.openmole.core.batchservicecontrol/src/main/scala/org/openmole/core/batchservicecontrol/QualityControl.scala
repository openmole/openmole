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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batchservicecontrol

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class QualityControl extends IQualityControl {

  val _failureRate = new AtomicInteger
  val _quality = new AtomicLong

  override def failed = _failureRate.incrementAndGet
    
  override def success = _failureRate.decrementAndGet
    
  override def failureRate: Int = _failureRate.get
    
  override def reinit = _failureRate.set(0)
    
  override def increaseQuality(value: Int) = _quality.addAndGet(value)
  override def decreaseQuality(value: Int) = _quality.addAndGet(-value)
  override def quality: Long = _quality.get
}
