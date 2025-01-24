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

import java.io.File

import org.openmole.core.workflow.execution.ExecutionState
import org.openmole.core.communication.storage._

object BatchJobControl:

  def tryStdOutErr(batchJob: BatchJobControl)(using priority: AccessControl.Priority) = util.Try(batchJob.stdOutErr(priority))
  def download(batchJobControl: BatchJobControl)(from: String, to: File, option: TransferOptions)(using priority: AccessControl.Priority) = batchJobControl.download(from, to, option, priority)
  def updateState(batchJobControl: BatchJobControl)(using priority: AccessControl.Priority) = batchJobControl.updateState(priority)
  def delete(batchJobControl: BatchJobControl)(using priority: AccessControl.Priority) = batchJobControl.delete(priority)

//  def apply(
//    updateInterval: () ⇒ UpdateInterval,
//    storageId:      () ⇒ String,
//    updateState:    AccessControl.Priority ⇒ ExecutionState,
//    delete:         AccessControl.Priority ⇒ Unit,
//    stdOutErr:      AccessControl.Priority ⇒ (String, String),
//    resultPath:     () ⇒ String,
//    download:       (String, File, TransferOptions, AccessControl.Priority) ⇒ Unit,
//    clean:          AccessControl.Priority ⇒ Unit): BatchJobControl = new BatchJobControl(
//    updateInterval,
//    storageId,
//    updateState,
//    delete,
//    stdOutErr,
//    download,
//    resultPath,
//    clean)


class BatchJobControl(
  val updateInterval: () ⇒ UpdateInterval,
  val storageId:      () ⇒ String,
  val updateState:    AccessControl.Priority ⇒ ExecutionState,
  val delete:         AccessControl.Priority ⇒ Unit,
  val stdOutErr:      AccessControl.Priority ⇒ (String, String),
  val download:       (String, File, TransferOptions, AccessControl.Priority) ⇒ Unit,
  val resultPath:     () ⇒ String,
  val clean:          AccessControl.Priority ⇒ Unit)
