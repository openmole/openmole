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

package org.openmole.core.implementation.transition;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.implementation.mole.SubMoleExecution;
import org.openmole.core.implementation.task.ExplorationTask;
import org.openmole.core.implementation.tools.ClonningService;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.plan.IExploredPlan;
import org.openmole.core.model.plan.IFactorValues;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.transition.IExplorationTransition;
import org.openmole.core.model.transition.IGenericTransition;
import org.openmole.core.model.capsule.IExplorationTaskCapsule;
import org.openmole.core.model.tools.IRegistryWithTicket;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.ITicket;
import org.openmole.core.model.mole.IMoleExecution;
import org.openmole.core.model.mole.ISubMoleExecution;

public class ExplorationTransition extends GenericTransition<IExplorationTaskCapsule> implements IExplorationTransition {

    public ExplorationTransition(IExplorationTaskCapsule start, IGenericTaskCapsule<?, ?> end) {
        super(start, end);
    }

    @Override
    public synchronized void performImpl(IContext global, IContext context, ITicket ticket, Set<String> toClone, IMoleExecution moleExecution, ISubMoleExecution subMole) throws InternalProcessingError, UserBadDataError {

        IRegistryWithTicket<IGenericTransition, IContext> registry = moleExecution.getLocalCommunication().getTransitionRegistry();
        registry.register(this, ticket, context);

        IExploredPlan values = context.getValue(ExplorationTask.ExploredPlan.getPrototype());
        context.removeVariable(ExplorationTask.ExploredPlan.getPrototype().getName());

        ISubMoleExecution subSubMole = new SubMoleExecution(moleExecution, subMole);

        int size = 0;

        Iterator<IFactorValues> factorIt = values.iterator();

        while(factorIt.hasNext()) {
            IFactorValues value = factorIt.next();

            size++;
            subSubMole.incNbJobInProgress();

            ITicket newTicket = moleExecution.nextTicket(ticket);
            IContext destContext = new Context();

            Set<IData<?>> notFound = new HashSet<IData<?>>();

            for (IData<?> in : getEnd().getCapsule().getTask().getInput()) {
                //Check filtred here to avoid useless clonning
                if (context.contains(in.getPrototype()) && !getFiltred().contains(in.getPrototype().getName())) {

                    IVariable<?> varToClone = context.getVariable(in.getPrototype());
                    IVariable<?> var;

                    if(!in.getMode().isImmutable() && factorIt.hasNext()) {
                        var = ClonningService.clone(varToClone);
                    } else {
                        var = varToClone;
                    }

                    destContext.putVariable(var);
                } else {
                    notFound.add(in);
                }
            }


            for(IData data : notFound) {
                IPrototype prototype = data.getPrototype();
                Object val = value.getValue(data.getPrototype());

                if (val != null) {
                    if (prototype.getType().isAssignableFrom(val.getClass())) {
                        destContext.putVariable(prototype, val);
                    } else {
                        throw new UserBadDataError("Factor variable " + prototype.getName() + " of type " + val.getClass().getName() + " found but not compatible with expected type for input " + prototype.getType().getName());
                    }
                }
            }
            
            submitNextJobsIfReady(global, destContext, newTicket, toClone, moleExecution, subSubMole);
        }

        subSubMole.decNbJobInProgress(size);
    }

    @Override
    protected void plugStart() {
        getStart().plugOutputTransition(this);
    }
}
