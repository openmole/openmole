/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.job;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.commons.exception.ExecutionException;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.commons.exception.UserBadDataError;

public class Job implements IJob {

    Map<IMoleJobId, IMoleJob> moleJobs = Collections.synchronizedMap(new TreeMap<IMoleJobId, IMoleJob>());

    @Override
    public Iterable<IMoleJob> getMoleJobs() {
        return moleJobs.values();
    }

    @Override
    public void rethrowException(IMoleJobId id, IContext context) throws ExecutionException {
        getMoleJob(id).rethrowException(context);
    }

    @Override
    public void finished(IMoleJobId id, IContext context) throws UserBadDataError, InternalProcessingError {
        getMoleJob(id).finished(context);
    }

    @Override
    public boolean isFinished(IMoleJobId id) {
        return getMoleJob(id).isFinished();
    }

    IMoleJob getMoleJob(IMoleJobId id) {
        return moleJobs.get(id);
    }

    public void addMoleJob(IMoleJob moleJob) {
        moleJobs.put(moleJob.getId(), moleJob);
    }

    @Override
    public int size() {
        return moleJobs.size();
    }

    @Override
    public boolean allMoleJobsFinished() {
        for(IMoleJob moleJob : getMoleJobs()) {
            if(!moleJob.isFinished()) {
                return false;
            }
         }
        return true;
    }

}
