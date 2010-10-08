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

package org.openmole.core.model.job;

import org.openmole.commons.exception.ExecutionException;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public interface IJob {
    Iterable<IMoleJob> getMoleJobs();
    int size();
    void rethrowException(IMoleJobId moleJob, IContext context) throws ExecutionException;
    void finished(IMoleJobId moleJob, IContext context) throws UserBadDataError, InternalProcessingError;
    boolean isFinished(IMoleJobId moleJob);
    boolean allMoleJobsFinished();
}
