/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.mole;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.job.ITicket;

public interface IMoleExecution {

    final public static String starting = "starting";
    final public static String finished = "finished";
    final public static String oneJobSubmitted = "oneJobSubmitted";
    final public static String oneJobFinished = "oneJobFinished";

    void start() throws InternalProcessingError, UserBadDataError;
    void cancel() throws InternalProcessingError, UserBadDataError;

    void waitUntilEnded() throws InterruptedException;
    boolean isFinished();

    void submit(IGenericTaskCapsule<?, ?> capsule, IContext global, IContext context, ITicket ticket, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError;

    IMole getMole();

    ITicket createRootTicket();

    ITicket nextTicket(ITicket parent);

    IMoleJobId nextJobId();

    ILocalCommunication getLocalCommunication();

    ISubMoleExecution getSubMoleExecution(IMoleJob job);
    
    ITicket getTicket(IMoleJob job);
    
    Iterable<IMoleJob> getAllMoleJobs();
}
