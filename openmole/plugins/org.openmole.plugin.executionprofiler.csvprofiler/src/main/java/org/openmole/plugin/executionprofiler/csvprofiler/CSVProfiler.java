/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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
package org.openmole.plugin.executionprofiler.csvprofiler;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.openmole.core.workflow.implementation.mole.MoleExecutionProfiler;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

import java.util.Collection;
import java.util.Iterator;
import org.openmole.core.workflow.implementation.task.GenericTask;
import org.openmole.core.workflow.model.job.ITimeStamp;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class CSVProfiler extends MoleExecutionProfiler {

    final CSVWriter writer;

    public CSVProfiler(IMoleExecution moleExecution) {
        this(moleExecution, new OutputStreamWriter(System.out));
    }

    public CSVProfiler(IMoleExecution moleExecution, Writer out) {
        super(moleExecution);
        writer = new CSVWriter(out);
    }

    @Override
    protected void moleJobFinished(IMoleJob moleJob) throws InternalProcessingError, UserBadDataError {
        Collection<ITimeStamp> timeStamps = moleJob.getContext().getLocalValue(GenericTask.Timestamps.getPrototype());
        String[] toWrite = new String[(timeStamps.size() - 1) * 3 + 2];

        int cur = 0;

        toWrite[cur++] = moleJob.getTask().getName();

        //get created
        Iterator<ITimeStamp> itTimeStamps = timeStamps.iterator();
        Long created = itTimeStamps.next().getTime();
        toWrite[cur++] = created.toString();


        while (itTimeStamps.hasNext()) {
            ITimeStamp timeStamp = itTimeStamps.next();
            toWrite[cur++] = timeStamp.getState().toString();
            toWrite[cur++] = timeStamp.getHostName();
            toWrite[cur++] = new Long(timeStamp.getTime() - created).toString();
        }

        writer.writeNext(toWrite);
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Override
    protected void moleExecutionFinished() throws InternalProcessingError, UserBadDataError {
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        }
    }
}
