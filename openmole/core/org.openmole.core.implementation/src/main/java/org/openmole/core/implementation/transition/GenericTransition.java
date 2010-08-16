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

package org.openmole.core.implementation.transition;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.job.Context;

import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.ICondition;
import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.data.IDataChannel;
import org.openmole.core.model.tools.IRegistryWithTicket;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ISubMoleExecution;
import org.openmole.core.model.transition.ISlot;


import static org.openmole.core.implementation.tools.ContextAggregator.*;

public abstract class GenericTransition<TS extends IGenericTaskCapsule<?, ?>> implements IGenericTransition<TS> {

    TS start;
    ISlot end;
    ICondition condition;

    Set<String> filtred = new TreeSet<String>();

    public GenericTransition(TS start, IGenericTaskCapsule end) {
        this(start, end.getDefaultInputSlot(), (ICondition)null);
    }

    public GenericTransition(TS start, IGenericTaskCapsule end, ICondition condition) {
        this(start, end.getDefaultInputSlot(), condition);
    }

    public GenericTransition(TS start, IGenericTaskCapsule end, String condition) {
        this(start, end.getDefaultInputSlot(), condition);
    }

    public GenericTransition(TS start, ISlot slot, String condition) {
        this(start, slot, new Condition(condition));
    }

    public GenericTransition(TS start, ISlot slot, ICondition condition) {
        this.start = start;
        this.end = slot;
        this.condition = condition;
        plugStart();
        slot.plugTransition(this);
    }

    @Override
    public TS getStart() {
        return start;
    }

    @Override
    public ISlot getEnd() {
        return end;
    }

    boolean nextTaskReady(IContext context, ITicket ticket, IMoleExecution execution) throws InternalProcessingError {
        IRegistryWithTicket<IGenericTransition, IContext> registry = execution.getLocalCommunication().getTransitionRegistry();

        for (IGenericTransition t : getEnd().getTransitions()) {
            if (!registry.isRegistredFor(t, ticket)) {
                return false;
            }
        }
        return true;
    }

    protected synchronized void submitNextJobsIfReady(IContext global, IContext context, ITicket ticket, Set<String> toClonne, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
             
        /*-- Remove filtred --*/
        for(String name: getFiltred()) {
           context.removeVariable(name);
       }

        IRegistryWithTicket<IGenericTransition, IContext> registry = moleExecution.getLocalCommunication().getTransitionRegistry();
        registry.register(this, ticket, context);

        if (nextTaskReady(context, ticket, moleExecution)) {
            Collection<IContext> combinaison = new LinkedList<IContext>();

            for (IGenericTransition t : getEnd().getTransitions()) {
                combinaison.add(registry.removeFromRegistry(t, ticket));
            }

            Iterable<IDataChannel> itDc = getEnd().getCapsule().getInputDataChannels();
            for (IDataChannel dataChannel : itDc) {
                IContext dataChannelContext = dataChannel.consums(context, ticket, toClonne, moleExecution);
                combinaison.add(dataChannelContext);
            }

            ITicket newTicket;

            if (getEnd().getCapsule().getIntputTransitionsSlots().size() <= 1) {
                newTicket = ticket;
            } else {
                newTicket = moleExecution.nextTicket(ticket.getParent());
            }

            //Agregate the variables = inputs for the next job
            IContext newContextEnd = new Context();

            aggregate(newContextEnd, getEnd().getCapsule().getAssignedTask().getInput(), toClonne, false, combinaison);

           // Logger.getLogger(Transition.class.getName()).info("Submit job for task " + getEnd().getCapsule().getTask().getName());
            moleExecution.submit(getEnd().getCapsule(),  global, newContextEnd, newTicket, subMole);
        }
    }

    @Override
    public void perform(IContext global, IContext context, ITicket ticket, Set<String> toClone, IMoleExecution scheduler, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {
        if (isConditionTrue(global, context)) {
            performImpl(global, context, ticket, toClone, scheduler, subMole);
        }
    }

    @Override
    public void setCondition(ICondition condition) {
        this.condition = condition;
    }

    @Override
    public ICondition getCondition() {
        return condition;
    }

    @Override
    public boolean isConditionTrue(IContext global, IContext context) throws UserBadDataError, InternalProcessingError {
        if (condition == null) {
            return true;
        }
        return condition.evaluate(global, context);
    }

    @Override
    public void setStart(TS task) {
        start = task;
    }

    @Override
    public void setEnd(ISlot task) {
        end = task;
    }

    @Override
    public Set<String> getFiltred() {
        return filtred;
    }

    @Override
    public void addFilter(IPrototype prototype) {
        addFilter(prototype.getName());
    }

    @Override
    public void addFilter(String name) {
        filtred.add(name);
    }

    @Override
    public void removeFilter(IPrototype prototype) {
        removeFilter(prototype.getName());
    }

    @Override
    public void removeFilter(String name) {
        filtred.remove(name);
    }

    
    abstract protected void performImpl(IContext global, IContext context, ITicket ticket, Set<String> toClone, IMoleExecution scheduler, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError;
    abstract protected void plugStart();
}
