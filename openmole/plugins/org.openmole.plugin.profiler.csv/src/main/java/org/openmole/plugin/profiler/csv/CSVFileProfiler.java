/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.profiler.csv;

import org.openmole.core.implementation.mole.Profiler;
import au.com.bytecode.opencsv.CSVWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;

import static org.openmole.plugin.profiler.csv.internal.MoleJobInfoToColumns.toColumns;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class CSVFileProfiler extends Profiler {

    transient CSVWriter writer;
    final File file;

    public CSVFileProfiler(IMoleExecution moleExecution, File file) {
        super(moleExecution);
        this.file = file;
    }

    public CSVFileProfiler(IMoleExecution moleExecution, String file) {
        this(moleExecution, new File(file));
    }


    @Override
    public void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
        writer.writeNext(toColumns(moleJob));
    }

    @Override
    public void moleExecutionFinished() throws InternalProcessingError, UserBadDataError {
        try {
            writer.close();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        } finally {
            writer = null;
        }
    }

    @Override
    public void moleExecutionStarting() throws InternalProcessingError, UserBadDataError {
        try {
            writer = new CSVWriter(new BufferedWriter(new FileWriter(file)));
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

}
