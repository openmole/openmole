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

package org.openmole.core.batch.control

import org.openmole.misc.tools.service._

object QualityControl {

  def withFailureControl[A](qualityControl: QualityControl)(op: ⇒ A): A = {
    try {
      val ret = op
      qualityControl.success
      ret
    } catch {
      case e: Throwable ⇒
        qualityControl.failed
        throw e
    }
  }

  def timed[A](qualityControl: QualityControl, op: ⇒ A): A = {
    val begin = System.currentTimeMillis
    val a = op
    qualityControl.timed(System.currentTimeMillis - begin)
    a
  }

  def apply(hysteresis: Int, nbSuccess: Int) = {
    val q = new QualityControl(hysteresis)
    for (i ← 0 until nbSuccess) q.success
    q
  }

}

class QualityControl(hysteresis: Int) {
  private val _successRate = new MovingAverage(hysteresis, 1.)
  private val operationTime = new MovingAverage(hysteresis)

  def failed = _successRate.apply(0)
  def success = _successRate.apply(1)
  def successRate = _successRate.get
  def reinit = { _successRate.reset(1.); operationTime.reset() }
  def timed(t: Double) = operationTime(t)
}
