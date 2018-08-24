/*
 * Copyright (C) 2010 Romain Reuillon
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
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

package org.openmole.plugin.environment.batch.environment

import org.openmole.core.workflow.execution.ExecutionState._
import org.openmole.plugin.environment.batch.storage.StorageService

object BatchJobControl {

  def tryStdOutErr(batchJob: BatchJobControl) = util.Try(batchJob.stdOutErr())

  def apply(
    storageService: StorageService[_],
    updateState:    () ⇒ ExecutionState,
    delete:         () ⇒ Unit,
    stdOutErr:      () ⇒ (String, String),
    resultPath:     () ⇒ String,
    clean:          () ⇒ Unit): BatchJobControl = new BatchJobControl(
    storageService,
    updateState,
    delete,
    stdOutErr,
    resultPath,
    clean)

}

class BatchJobControl(
  val storage:     StorageService[_],
  val updateState: () ⇒ ExecutionState,
  val delete:      () ⇒ Unit,
  val stdOutErr:   () ⇒ (String, String),
  val resultPath:  () ⇒ String,
  val clean:       () ⇒ Unit)
