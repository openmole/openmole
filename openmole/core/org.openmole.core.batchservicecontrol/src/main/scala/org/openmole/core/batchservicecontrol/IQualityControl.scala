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

import org.openmole.core.batchservicecontrol.internal.Activator
import org.openmole.core.model.execution.batch.BatchServiceDescription

object IQualityControl {

  def withFailureControl[A](desc: BatchServiceDescription, op: => A): A = {
    val qualityControl = Activator.getRessourceControl.qualityControl(desc)
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
}

trait IQualityControl {
    def failed
    def success
    def failureRate: Int
    def reinit
    def increaseQuality(value: Int)
    def decreaseQuality(value: Int)
    def quality: Int
}
