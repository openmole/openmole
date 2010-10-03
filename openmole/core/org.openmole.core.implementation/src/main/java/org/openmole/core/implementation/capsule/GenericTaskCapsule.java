/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.core.implementation.capsule;

import org.openmole.core.implementation.transition.SlotSet;
import org.openmole.core.implementation.transition.Slot;
import org.openmole.core.implementation.job.MoleJob;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.service.Priority;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.transition.ISlot;
import org.openmole.core.model.job.IMoleJobId;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.mole.ExecutionInfoRegistry;
import org.openmole.core.implementation.transition.MultiOut;

import org.openmole.core.model.task.IGenericTask;
import org.openmole.core.model.data.IDataChannel;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ISubMoleExecution;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;

import static org.openmole.core.implementation.tools.ToCloneFinder.getVariablesToClone;

public abstract class GenericTaskCapsule<TOUT extends IGenericTransition, TASK extends IGenericTask> implements IGenericTaskCapsule<TOUT, TASK> {

    class GenericTaskCapsuleAdapter implements IObjectChangedSynchronousListener<MoleJob> {

        @Override
        public void objectChanged(MoleJob obj) throws InternalProcessingError, UserBadDataError {
            switch (obj.getState()) {
                case COMPLETED:
                    jobFinished(obj);
                    break;
            }
        }
    }
    private SlotSet inSlotSet;
    private ISlot defaultSlot;
    private MultiOut<TOUT> out;
    private Set<IDataChannel> inputDataChannels;
    private Set<IDataChannel> outputDataChannels;
    private TASK task;

    public GenericTaskCapsule(TASK task) {
        super();

        this.task = task;

        this.defaultSlot = new Slot(this);
        this.inSlotSet = new SlotSet();
        this.out = new MultiOut<TOUT>();

        addInputTransitionSlot(defaultSlot);

        this.inputDataChannels = new HashSet<IDataChannel>();
        this.outputDataChannels = new HashSet<IDataChannel>();
    }

    @Override
    public void addInputTransitionSlot(ISlot group) {
        inSlotSet.addInputTransitionGroup(group);
    }

    @Override
    public ISlot getDefaultInputSlot() {
        return defaultSlot;
    }

    @Override
    public Collection<ISlot> getIntputTransitionsSlots() {
        return inSlotSet.getGroups();
    }

    @Override
    public Iterable<IDataChannel> getInputDataChannels() {
        return inputDataChannels;
    }

    @Override
    public Collection<IDataChannel> getOutputDataChannels() {
        return outputDataChannels;
    }

    @Override
    public void plugInputDataChannel(IDataChannel dataChannel) {
        dataChannel.setEnd(this);
        inputDataChannels.add(dataChannel);
    }

    @Override
    public void plugOutputDataChannel(IDataChannel dataChannel) {
        dataChannel.setStart(this);
        outputDataChannels.add(dataChannel);
    }

    @Override
    public void unplugInputDataChannel(IDataChannel dataChannel) {
        dataChannel.setEnd(null);
        inputDataChannels.remove(dataChannel);
    }

    @Override
    public void unplugOutputDataChannel(IDataChannel dataChannel) {
        dataChannel.setStart(null);
        outputDataChannels.remove(dataChannel);
    }


    @Override
    public void plugOutputTransition(TOUT transition) {
        assert transition != null;
        getOut().addOuputTransition(transition);
    }


    @Override
    public IMoleJob toJob(IContext global, IContext context, IMoleJobId jobId) throws InternalProcessingError, UserBadDataError {
        MoleJob ret = new MoleJob(getAssignedTask(), global, context,jobId);

        Activator.getEventDispatcher().registerListener(ret, Priority.LOW.getValue(), new GenericTaskCapsuleAdapter(), IMoleJob.StateChanged);
        Activator.getEventDispatcher().objectChanged(this, JobCreated, new Object[]{ret});
        return ret;
    }

    @Override
    public TASK getTask() {
        return task;
    }

    @Override
    public TASK getAssignedTask() throws UserBadDataError {
        TASK ret = getTask();
        if (ret == null) {
            throw new UserBadDataError("Task has not been assgned");
        }
        return ret;
    }

    @Override
    public void setTask(TASK task) {
        this.task = task;
    }

    private void jobFinished(MoleJob job) throws InternalProcessingError, UserBadDataError {
        try {
            IMoleExecution execution = ExecutionInfoRegistry.GetInstance().remove(job);
            ISubMoleExecution subMole = execution.getSubMoleExecution(job);
            ITicket ticket = execution.getTicket(job);

            performTransition(job.getGlobalContext(), job.getContext(), ticket, execution, subMole);
        } catch (InternalProcessingError e) {
            throw new InternalProcessingError(e, "Error at the end of a MoleJob for task " + getAssignedTask());
        } catch (UserBadDataError e) {
            throw new UserBadDataError(e, "Error at the end of a MoleJob for task " + getAssignedTask());
        } finally {
            Activator.getEventDispatcher().objectChanged(job, MoleJob.TransitionPerformed);
        }
    }

    protected void performTransition(IContext global, IContext context, ITicket ticket, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        for (IDataChannel dataChannel : getOutputDataChannels()) {
            dataChannel.provides(context, ticket, getVariablesToClone(this, global, context), moleExecution);
        }

        performTransitionImpl(global, context, ticket, moleExecution, subMole);
    }

    @Override
    public Collection<TOUT> getOutputTransitions() {
        return getOut().getOutputTransitions();
    }

    protected MultiOut<TOUT> getOut() {
        return out;
    }

    @Override
    public String toString() {
        TASK task = getTask();

        if (task == null) {
            return "";
        }
        return task.toString();
    }



    public void performTransitionImpl(IContext global, IContext context, ITicket ticket, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        Collection<TOUT> outputTransitions = getOutputTransitions();

        if (outputTransitions.size() == 1 && getOutputDataChannels().isEmpty()) {
            outputTransitions.iterator().next().perform(global, context, ticket, Collections.EMPTY_SET, moleExecution, subMole);
        } else {
            for (TOUT transition : outputTransitions) {
                transition.perform(global, context, ticket, getVariablesToClone(this, global, context), moleExecution, subMole);
            }
        }
    }


}
