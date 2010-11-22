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

package org.openmole.core.batchservicecontrol.internal

import org.openmole.core.batchservicecontrol.AccessToken
import org.openmole.core.batchservicecontrol.IBatchServiceControl
import org.openmole.core.batchservicecontrol.IQualityControl
import org.openmole.core.batchservicecontrol.IUsageControl
import org.openmole.core.batchservicecontrol.UsageControl
import org.openmole.core.model.execution.batch.BatchServiceDescription
import scala.collection.immutable.TreeMap

object BatchServiceControl {
  val botomlessUsage = new UsageControl(BotomlessTokenPool)
}

class BatchServiceControl extends IBatchServiceControl {
 
  var ressources = new TreeMap[BatchServiceDescription, (IUsageControl, IQualityControl)]

  override def registerRessouce(ressource: BatchServiceDescription, usageControl: IUsageControl, failureControl: IQualityControl) = synchronized {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._2.reinit
      case None => ressources += ((ressource -> (usageControl, failureControl)))
    }  
  }

  override def qualityControl(ressource: BatchServiceDescription): Option[IQualityControl] = {
    ressources.get(ressource) match {
      case Some(ctrl) => Some(ctrl._2)
      case None => None
    }
  } 
  
  def usageControl(ressource: BatchServiceDescription): IUsageControl = {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._1
      case None => BatchServiceControl.botomlessUsage
    }
  }

}
