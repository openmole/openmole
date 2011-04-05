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

package org.openmole.plugin.profiler.csv

import au.com.bytecode.opencsv.CSVWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.openmole.core.implementation.hook.Profiler
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.plugin.profiler.csv.MoleJobInfoToCSV._
import org.openmole.core.model.job.State._
import scala.ref.WeakReference

class CSVFileProfiler(moleExecution:  WeakReference[IMoleExecution], file: File) extends Profiler(moleExecution) {

  def this(moleExecution: IMoleExecution, file: String) = this(new WeakReference(moleExecution), new File(file))

  def this(moleExecution: IMoleExecution, file: File) = this(new WeakReference(moleExecution), file)

  file.getParentFile.mkdirs
  @transient lazy val writer = new CSVWriter(new BufferedWriter(new FileWriter(file)))
  
  override def stateChanged(moleJob: IMoleJob) =  {
    if(moleJob.state.isFinal) {
      writer.writeNext(toColumns(moleJob))
      writer.flush
    }
  }

  override def executionFinished = writer.close

}
