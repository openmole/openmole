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

package org.openmole.plugin.profiler.csv

import au.com.bytecode.opencsv.CSVWriter
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import org.openmole.core.model.job._
import org.openmole.core.model.mole._
import org.openmole.misc.tools.io.FileUtil._
import ToCSV._

object CSVFileProfiler {
  def apply(file: File) = new CSVFileProfiler(file)
}

class CSVFileProfiler(file: File) extends Profiler {

  override def process(moleJob: IMoleJob, executionContext: ExecutionContext) = synchronized {
    val f = executionContext.directory.child(file)

    f.getParentFile.mkdirs
    val writer = new CSVWriter(new BufferedWriter(new FileWriter(f)))

    try writer.writeNext(toColumns(moleJob))
    finally writer.close
  }

}
