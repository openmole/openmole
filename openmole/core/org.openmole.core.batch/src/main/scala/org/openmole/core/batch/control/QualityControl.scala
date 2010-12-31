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

package org.openmole.core.batch.control

import org.openmole.commons.tools.internal.Activator._
import org.openmole.commons.tools.service.MoovingAverage

object QualityControl {
  
  def withQualityControl[A](qualityControl: Option[QualityControl], op: => A, isFailure: Throwable => Boolean): A = {
    try {
      val ret = op
      qualityControl match {
        case Some(f) => f.success
        case None => 
      }
      ret
    } catch {
      case e =>
        qualityControl match {
          case Some(f) => f.failed
          case None =>
        }
        throw e
    }
  }
  
  def withQualityControl[A](qualityControl: QualityControl, op: => A, isFailure: Throwable => Boolean): A = {
    try {
      val ret = op
      qualityControl.success
      ret
    } catch {
      case e =>
        if(isFailure(e)) qualityControl.failed
        throw e
    }
  }
  
}

class QualityControl(hysteresis: Int) {
  val _successRate = new MoovingAverage(hysteresis)
  success

  def failed = _successRate.apply(0)
  def success = _successRate.apply(1)
  def successRate = _successRate.get
  def reinit = _successRate.clear
}
