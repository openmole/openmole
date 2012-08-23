/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.hook.csvprofiler

import au.com.bytecode.opencsv.CSVWriter
import java.io.OutputStreamWriter
import java.io.Writer
import org.openmole.core.implementation.hook.MoleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State._
import org.openmole.core.model.mole.IMoleExecution
import ToCSV._
import scala.ref.WeakReference

class CSVProfiler(val moleExecution: WeakReference[IMoleExecution], writer: CSVWriter) extends MoleExecutionHook {

  def this(moleExecution: IMoleExecution, out: Writer) = this(new WeakReference(moleExecution), new CSVWriter(out))

  def this(moleExecution: IMoleExecution) = this(new WeakReference(moleExecution), new CSVWriter(new OutputStreamWriter(System.out)))

  override def stateChanged(moleJob: IMoleJob, newState: State, oldState: State) = synchronized {
    if (moleJob.state.isFinal) {
      writer.writeNext(toColumns(moleJob))
      writer.flush
    }
  }

  override def executionFinished = writer.flush

}
