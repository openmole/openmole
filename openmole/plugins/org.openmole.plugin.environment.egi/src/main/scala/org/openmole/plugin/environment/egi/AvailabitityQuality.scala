/*
 * Copyright (C) 13/11/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.egi

import org.openmole.core.batch.control._

object AvailabilityQuality {
  def apply(_usageControl: UsageControl, _hysteresis: Int) =
    new AvailabilityQuality {
      val usageControl = _usageControl
      val hysteresis = _hysteresis
    }
}

trait AvailabilityQuality extends QualityControl with UsageControl {

  val usageControl: UsageControl

  def available: Int = usageControl.available
  def releaseToken(token: AccessToken): Unit = usageControl.releaseToken(token)
  def waitAToken: AccessToken = timedWait(usageControl.waitAToken)
  def tryGetToken = usageControl.tryGetToken

}
