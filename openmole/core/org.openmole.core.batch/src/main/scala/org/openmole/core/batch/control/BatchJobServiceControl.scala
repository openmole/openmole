/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

import org.openmole.commons.exception.InternalProcessingError
import scala.collection.immutable.HashMap


object BatchJobServiceControl {
  var ressources = new HashMap[BatchJobServiceDescription, (UsageControl, JobServiceQualityControl)]

  def registerRessouce(ressource: BatchJobServiceDescription, usageControl: UsageControl, failureControl: JobServiceQualityControl) = synchronized {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._2.reinit
      case None => ressources += ((ressource -> (usageControl, failureControl)))
    }  
  }

  def qualityControl(ressource: BatchJobServiceDescription): JobServiceQualityControl = {
    ressources.getOrElse(ressource, throw new InternalProcessingError("Quality control not found for " + ressource.toString))._2
  } 
  
  def usageControl(ressource: BatchJobServiceDescription): UsageControl = {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._1
      case None => UsageControl.botomlessUsage
    }    
  }
  
  
  def withFailureControl[A](desc: BatchJobServiceDescription, op: => A): A = {
    val qualityControl = this.qualityControl(desc)
    QualityControl.withQualityControl(qualityControl, op)
  }
  
   
  def withToken[B]( desc: BatchJobServiceDescription, f: (AccessToken => B)): B = {
    val usageControl = this.usageControl(desc)
    UsageControl.withUsageControl(usageControl, f)
  }
  
}
