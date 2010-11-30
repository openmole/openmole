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

package org.openmole.core.batch.environment

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batch.control.BatchServiceControl
import org.openmole.core.batch.control.BatchServiceDescription
import org.openmole.core.batch.control.IQualityControl
import org.openmole.core.batch.control.IUsageControl

abstract class BatchService [ENV <: IBatchEnvironment, AUTH <: IBatchServiceAuthentication](val description: BatchServiceDescription, val environment: ENV, val authenticationKey: IBatchServiceAuthenticationKey[AUTH]) extends IBatchService[ENV, AUTH] {

  def this(description: BatchServiceDescription, environment: ENV, authenticationKey: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH, usageControl: IUsageControl, failureControl: IQualityControl) = {
    this(description, environment, authenticationKey)
    AuthenticationRegistry.initAndRegisterIfNotAllreadyIs(authenticationKey, authentication)
    BatchServiceControl.registerRessouce(description, usageControl, failureControl)      
  }
  
  override def authentication: AUTH = {
    AuthenticationRegistry.registred(authenticationKey) match {
      case None => throw new InternalProcessingError("No authentication registred for batch service")
      case Some(a) => a
    }
  }

  override def toString: String = description.toString
    
}
