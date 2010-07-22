/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.core.implementation.transition;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;


import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.service.Priority;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.implementation.job.Context;


import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.IAggregationTransition;
import org.openmole.core.model.tools.IRegistryWithTicket;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.capsule.ITaskCapsule;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ISubMoleExecution;

import static org.openmole.core.implementation.tools.ContextAggregator.aggregate;
import static org.openmole.core.implementation.tools.ContextAggregator.findDataIn1WhichAreAlsoIn2;
import static org.openmole.core.implementation.tools.ToCloneFinder.getVariablesToClone;

public class AggregationTransition extends SingleTransition implements IAggregationTransition {

    public class AggregationTransitionAdapter implements IObjectChangedSynchronousListenerWithArgs<ISubMoleExecution> {

        @Override
        public void objectChanged(ISubMoleExecution subMole, Object[] args) throws InternalProcessingError, UserBadDataError {
            if (!IMoleJob.class.isAssignableFrom(args[0].getClass())) {
                throw new InternalProcessingError("BUG: argument of the event has the wrong type.");
            }

            IMoleJob lastJob = (IMoleJob) args[0];
            IMoleExecution moleExecution = (IMoleExecution) args[1];
            subMoleFinished(subMole, lastJob, moleExecution);
        }
    }

    public AggregationTransition(ITaskCapsule start, IGenericTaskCapsule<?, ?> end) {
        super(start, end);
    }

    @Override
    public synchronized void performImpl(IContext global, IContext context, ITicket ticket, Set<String> toClone, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {

        IRegistryWithTicket<IAggregationTransition, Collection<IContext>> registry = moleExecution.getLocalCommunication().getAggregationTransitionRegistry();

        //Create the results context for aggregation if not already done
        Collection<IContext> resultContexts;

        if (!registry.isRegistredFor(this, ticket.getParent())) {
            resultContexts = new LinkedList<IContext>();
            registry.register(this, ticket.getParent(), resultContexts);
            Activator.getEventDispatcher().registerListener(subMole, Priority.LOW.getValue(), new AggregationTransitionAdapter(), ISubMoleExecution.finished);
        } else {
            resultContexts = registry.consult(this, ticket.getParent());
        }

        //Store the result context
        resultContexts.add(context);
    }

    public void subMoleFinished(ISubMoleExecution subMole, IMoleJob job, IMoleExecution moleExecution) throws InternalProcessingError, UserBadDataError {
        IRegistryWithTicket <IAggregationTransition, Collection<IContext>> registry =  moleExecution.getLocalCommunication().getAggregationTransitionRegistry();

        ITicket newTicket = job.getTicket().getParent();
        IContext newContextEnd = new Context();

        Collection<IContext> resultContexts;
        resultContexts = registry.removeFromRegistry(this, newTicket);

        IDataSet dataToAggregate = findDataIn1WhichAreAlsoIn2(getEnd().getCapsule().getAssignedTask().getInput(), getStart().getAssignedTask().getOutput());
        
        Set<String> toClone = new TreeSet<String>();

        //Find the variable to clonne, it is function of the evaluation of condition on the transition
        for(IContext context: resultContexts) {
             toClone.addAll(getVariablesToClone(getStart(), job.getGlobalContext(), context));
        }
        aggregate(newContextEnd, dataToAggregate, toClone, true, resultContexts);

        //Variable have are clonned in other transitions if necessary
        submitNextJobsIfReady(job.getGlobalContext(), newContextEnd, newTicket, Collections.EMPTY_SET, moleExecution, subMole.getParent());
    }
}
