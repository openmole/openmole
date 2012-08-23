/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.csvprofiler

import au.com.bytecode.opencsv.CSVWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.openmole.core.implementation.hook.EnvironmentHook
import org.openmole.core.model.execution.ExecutionState.ExecutionState
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.misc.tools.io.FileUtil._

class CSVFileEnvironmentProfiler(environment: IEnvironment, file: File) extends EnvironmentHook(environment) {

  def this(environment: IEnvironment, file: String) = this(environment, new File(file))

  file.getParentFile.mkdirs
  file.delete

  override def jobStatusChanged(job: IExecutionJob, newState: ExecutionState, oldState: ExecutionState) = synchronized {
    if (newState.isFinal) {
      val writter = new CSVWriter(new BufferedWriter(new FileWriter(file, true)))
      try {

        val jobIds = job.moleJobs.map { _.id }.mkString(":")
        val (created, timeStampsStr) = ToCSV.toCSV(job.timeStamps)
        writter.writeNext((jobIds :: created.toString :: timeStampsStr.toList).toArray)
      } finally writter.close
    }
  }

}
