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

package org.openmole.core.workflow.model.mole;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.job.IMoleJobId;
import org.openmole.core.workflow.model.job.ITicket;
import org.openmole.core.workflow.model.mole.IMole;

public interface IMoleExecution {

    final public static String finished = "finished";
    final public static String oneJobJinished = "oneJobJinished";

    void start();
    void cancel() throws InternalProcessingError, UserBadDataError;

    void waitUntilEnded() throws InterruptedException;
    boolean isFinished();

    void submit(IGenericTaskCapsule<?, ?> capsule, IContext context, ITicket ticket, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError;
    void submit(IMoleJob job, IGenericTaskCapsule<?, ?> capsule, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError;

    int getLevel();
    IMole getMole();

    ITicket createRootTicket();

    ITicket nextTicket(ITicket parent);

    IMoleJobId nextJobId();

    ILocalCommunication getLocalCommunication();

    ISubMoleExecution getSubMoleExecution(IMoleJob job);

    IExecutionContext getExecutionContext();

    Iterable<IMoleJob> getAllMoleJobs();
}
