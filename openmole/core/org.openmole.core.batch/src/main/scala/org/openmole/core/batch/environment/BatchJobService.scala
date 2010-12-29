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

import org.openmole.core.batch.control.AccessToken
import org.openmole.core.batch.control.BatchJobServiceControl
import org.openmole.core.batch.control.BatchJobServiceControl._
import org.openmole.core.batch.control.BatchJobServiceDescription
import org.openmole.core.batch.control.JobServiceQualityControl
import org.openmole.core.batch.control.QualityControl
import org.openmole.core.batch.control.UsageControl
import org.openmole.core.batch.file.IURIFile

abstract class BatchJobService(authenticationKey: BatchAuthenticationKey, authentication: BatchAuthentication, val description: BatchJobServiceDescription, nbAccess: Int) extends BatchService(authenticationKey, authentication) {
  
  BatchJobServiceControl.registerRessouce(description, UsageControl(nbAccess), new JobServiceQualityControl)      

  def submit(inputFile: IURIFile, outputFile: IURIFile, runtime: Runtime, token: AccessToken): BatchJob = {
    withFailureControl(description, doSubmit(inputFile, outputFile, runtime, token))
  }
 
  protected def doSubmit(inputFile: IURIFile, outputFile: IURIFile, runtime: Runtime, token: AccessToken): BatchJob

  override def toString: String = description.toString
  
  def test: Boolean
}
