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

package org.openmole.plugin.executionprofiler.csv.internal;

import java.util.Collection;
import java.util.Iterator;
import org.openmole.core.implementation.task.GenericTask;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.ITimeStamp;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class MoleJobInfoToColumns {

    public static String[] toColumns(IMoleJob moleJob) {
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
        return toWrite;
    }
}
