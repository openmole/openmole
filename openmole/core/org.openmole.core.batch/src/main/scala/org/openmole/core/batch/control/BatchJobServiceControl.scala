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

import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.batch.environment.TemporaryErrorException
import scala.collection.immutable.HashMap


object JobServiceControl {
  var ressources = new HashMap[JobServiceDescription, (UsageControl, JobServiceQualityControl)]

  def registerRessouce(ressource: JobServiceDescription, usageControl: UsageControl, failureControl: JobServiceQualityControl) = synchronized {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._2.reinit
      case None => ressources += ((ressource -> (usageControl, failureControl)))
    }  
  }

  def qualityControl(ressource: JobServiceDescription): JobServiceQualityControl = {
    ressources.getOrElse(ressource, throw new InternalProcessingError("Quality control not found for " + ressource.toString))._2
  } 
  
  def usageControl(ressource: JobServiceDescription): UsageControl = {
    ressources.get(ressource) match {
      case Some(ctrl) => ctrl._1
      case None => UsageControl.botomlessUsage
    }    
  }
  
  def withFailureControl[A](desc: JobServiceDescription, op: => A): A = withFailureControl[A](desc, op, {e: Throwable => !classOf[TemporaryErrorException].isAssignableFrom(e.getClass)})
  def withFailureControl[A](desc: JobServiceDescription, op: => A, isFailure: Throwable => Boolean): A = {
    val qualityControl = this.qualityControl(desc)
    QualityControl.withQualityControl(qualityControl, op, isFailure)
  }
  
   
  def withToken[B]( desc: JobServiceDescription, f: (AccessToken => B)): B = {
    val usageControl = this.usageControl(desc)
    UsageControl.withUsageControl(usageControl, f)
  }
  
}
