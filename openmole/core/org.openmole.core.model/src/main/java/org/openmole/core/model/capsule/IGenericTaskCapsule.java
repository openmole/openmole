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
package org.openmole.core.model.capsule;

import java.util.Collection;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.data.IDataChannel;
import org.openmole.core.model.job.ITicket;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.core.model.transition.ITransition;
import org.openmole.core.model.transition.ITransitionSlot;

/**
 *
 * Common interface for all task capsules.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 * @param <TOUT> Type of the output transition.
 * @param <TASK> Type of the task inside the capsule.
 */
public interface IGenericTaskCapsule<TOUT extends ITransition, TASK extends IGenericTask> {

    public static final String JobCreated = "JobCreated";
    
    /**
     *
     * Get the task assigned to this capsule or null if not the task has not been assigned.
     *
     * @return task inside this capsule or null if not the task has not been assigned
     */
    TASK getTask();

    /**
     *
     * Get the task assigned to this capsule or throw an exception if not assigned.
     *
     * @return the task assigned to this capsule
     * @throws UserBadDataError if no task is assigned to this capsule
     */
    TASK getAssignedTask() throws UserBadDataError;

    /**
     *
     * Assigned a task to this capsule.
     *
     * @param task the task to assign.
     */
    void setTask(TASK task);

    /**
     *
     * Get the list of data channels ending at this capsule.
     *
     * @return the list of data channels ending at this capsule
     */
    Iterable<IDataChannel> getInputDataChannels();

    /**
     *
     * Get the list of data channels starting at this capsule.
     *
     * @return the list of data channels starting at this capsule
     */
    Iterable<IDataChannel> getOutputDataChannels();

    /**
     *
     * Plug a datachannel in input of this capsule.
     *
     * @param dataChannel the datachannel to plug
     */
    void plugInputDataChannel(IDataChannel dataChannel);

    /**
     *
     * Plug a datachannel in output of this capsule.
     *
     * @param dataChannel the datachannel to plug
     */
    void plugOutputDataChannel(IDataChannel dataChannel);

    /**
     *
     * Unplug a datachannel in input of this capsule.
     *
     * @param dataChannel   the datachannel to unplug
     */
    void unplugInputDataChannel(IDataChannel dataChannel);

    /**
     *
     * Unplug a datachannel in output of this capsule.
     *
     * @param dataChannel   the datachannel to unplug
     */
    void unplugOutputDataChannel(IDataChannel dataChannel);


    /**
     *
     * Get the default input slot of this capsule.
     *
     * @return the default input slot of this capsule
     */
    ITransitionSlot getDefaultInputSlot();

    /**
     *
     * Get all the input slots of this capsule.
     *
     * @return the input slots of this capsule
     */
    Collection<ITransitionSlot> getIntputTransitionsSlots();

    /**
     * 
     * Add <code>transition</code> in the default input slot to this capsule.
     * 
     * @param group the transition slot to be added.
     */
    void addInputTransitionSlot(ITransitionSlot group);


    /**
     *
     * Plug <code>transiton</code> in output of this capsule.
     *
     * @param transition the transition to plug
     */
    void plugOutputTransition(TOUT transition);

    /**
     *
     * Get all the output transitions plugged to this capsule.
     *
     * @return all the output transitions plugged to this capsule
     */
    Collection<TOUT> getOutputTransitions();

    /**
     *
     * Instanciate a job from this capsule.
     *
     * @param context the context in which the job will be executed
     * @param ticket the ticket in which the job will be executed
     * @param jobId the id of the job
     * @return the job
     * @throws InternalProcessingError if something goes wrong because of a system failure
     * @throws UserBadDataError if something goes wrong because it is missconfigured
     */
    IMoleJob toJob(IContext context, ITicket ticket, IMoleJobId jobId) throws InternalProcessingError, UserBadDataError;
}
