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
import org.openmole.core.batchservicecontrol.internal.Activator
import org.openmole.misc.workspace.ConfigurationLocation

object QualityControl {
  val MaxQuality = new ConfigurationLocation("QualityControl", "MaxQuality")
  Activator.getWorkspace += (MaxQuality, "100")
}


class QualityControl extends IQualityControl {

  val _failureRate = new AtomicInteger
  @volatile var _quality = 1

  override def failed = _failureRate.incrementAndGet
  override def success = _failureRate.decrementAndGet
  override def failureRate: Int = _failureRate.get
  override def reinit = _failureRate.set(0)
    
  override def increaseQuality(value: Int) = synchronized {
    _quality += value
    val max = Activator.getWorkspace.preferenceAsInt(QualityControl.MaxQuality)
    if(_quality > max) _quality = max
  }
  
  override def decreaseQuality(value: Int) = synchronized {
    _quality -= value
    if(_quality < 1) _quality = 1
  }
  
  override def quality: Int = _quality
}
