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

package org.openmole.core.implementation.execution.batch

import org.openmole.core.batchservicecontrol.IFailureControl
import org.openmole.core.batchservicecontrol.IUsageControl
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.batch.IBatchEnvironment
import org.openmole.core.model.execution.batch.IBatchService
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey
import org.openmole.core.model.execution.batch.IBatchServiceDescription


abstract class BatchService [ENV <: IBatchEnvironment[_], AUTH <: IBatchServiceAuthentication](val description: IBatchServiceDescription, val environment: ENV, val authenticationKey: IBatchServiceAuthenticationKey[AUTH]) extends IBatchService[ENV, AUTH] {

  def this(description: IBatchServiceDescription, environment: ENV, authenticationKey: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH, usageControl: IUsageControl, failureControl: IFailureControl) = {
    this(description, environment, authenticationKey)
    Activator.getBatchEnvironmentAuthenticationRegistry.initAndRegisterIfNotAllreadyIs(authenticationKey, authentication)
    Activator.getBatchRessourceControl.registerRessouce(description, usageControl, failureControl)      
  }
  
  override def authentication: AUTH = {
    Activator.getBatchEnvironmentAuthenticationRegistry.getRegistred(authenticationKey)
  }

  override def toString: String = {
    description.toString
  }
    
}
