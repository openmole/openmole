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

package org.openmole.core.implementation.execution.batch

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.batchservicecontrol.QualityControl
import org.openmole.core.batchservicecontrol.UsageControl
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.execution.batch.IAccessToken
import org.openmole.core.model.execution.batch.IBatchEnvironment
import org.openmole.core.model.execution.batch.IBatchJob
import org.openmole.core.model.execution.batch.IBatchJobService
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey
import org.openmole.core.model.execution.batch.BatchServiceDescription
import org.openmole.core.model.execution.batch.IRuntime
import org.openmole.core.model.file.IURIFile

abstract class BatchJobService[ENV <: IBatchEnvironment, AUTH <: IBatchServiceAuthentication](environment: ENV, authenticationKey: IBatchServiceAuthenticationKey[AUTH], authentication: AUTH, description: BatchServiceDescription, nbAccess: Int) extends BatchService[ENV, AUTH](description, environment, authenticationKey, authentication, UsageControl(nbAccess), new QualityControl) with IBatchJobService[ENV, AUTH] {

    override def submit(inputFile: IURIFile, outputFile: IURIFile, runtime: IRuntime, token: IAccessToken): IBatchJob = {
        try {
            val ret = doSubmit(inputFile, outputFile, runtime, token)
            Activator.getBatchRessourceControl.qualityControl(description) match {
              case Some(f) => f.success
              case None =>
            }
            return ret
        } catch {
          case (e: InternalProcessingError) =>
            Activator.getBatchRessourceControl.qualityControl(description) match {
              case Some(f) => f.failed
              case None =>
            }
            throw e
        }
    }
 
    protected def doSubmit(inputFile: IURIFile, outputFile: IURIFile, runtime: IRuntime, token: IAccessToken): IBatchJob

}
