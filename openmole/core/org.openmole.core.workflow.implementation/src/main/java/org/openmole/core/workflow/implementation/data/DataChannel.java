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
package org.openmole.core.workflow.implementation.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.model.tools.ILevelComputing;
import org.openmole.core.workflow.implementation.job.Context;
import org.openmole.core.workflow.implementation.internal.Activator;
import org.openmole.core.workflow.implementation.tools.LevelComputing;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.data.IDataChannel;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.core.workflow.model.data.IData;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.tools.IRegistryWithTicket;
import org.openmole.core.workflow.model.job.ITicket;
import org.openmole.core.workflow.model.mole.IMoleExecution;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public class DataChannel implements IDataChannel {

    IGenericTaskCapsule<?, ?> start;
    IGenericTaskCapsule<?, ?> end;
    Set<String> variblesNames = new TreeSet<String>();

    public DataChannel(IGenericTaskCapsule<?, ?> start, IGenericTaskCapsule<?, ?> end) {
        this.start = start;
        this.end = end;

        start.plugOutputDataChannel(this);
        end.plugInputDataChannel(this);
    }

    public DataChannel(IGenericTaskCapsule<?, ?> start, IGenericTaskCapsule<?, ?> end, String... variables) {
        this(start, end);

        for (String variable : variables) {
            add(variable);
        }

    }

    public DataChannel(IGenericTaskCapsule<?, ?> start, IGenericTaskCapsule<?, ?> end, Prototype... variables) {
        this(start, end);

        for (Prototype prototype : variables) {
            add(prototype);
        }
    }

    @Override
    public IGenericTaskCapsule<?, ?> getStart() {
        return start;
    }

    @Override
    public IGenericTaskCapsule<?, ?> getEnd() {
        return end;
    }

    @Override
    public IContext consums(IContext context, ITicket ticket, Set<String> toClonne, IMoleExecution moleExecution) throws InternalProcessingError {
        ILevelComputing levelComputing = LevelComputing.getLevelComputing(moleExecution);
        IRegistryWithTicket<IDataChannel, IContext> dataChannelRegistry = moleExecution.getLocalCommunication().getDataChannelRegistry();

        int startLevel = levelComputing.getLevel(getStart());
        int endLevel = levelComputing.getLevel(getEnd());

        int levelDif = endLevel - startLevel;
        if (levelDif < 0) {
            levelDif = 0;
        }

        ITicket currentTicket = ticket;

        for (int i = 0; i < levelDif; i++) {
            currentTicket = currentTicket.getParent();
        }

        IContext dataChannelContext = dataChannelRegistry.consult(this, currentTicket);

        if (dataChannelContext != null) {
            if (levelDif > 0) {
                for (IVariable<?> v : dataChannelContext) {
                    toClonne.add(v.getPrototype().getName());
                }
            }
        } else {
            throw new InternalProcessingError("No context registred for data channel found in input of task " + getEnd().toString());
        }

        return dataChannelContext;

    }

    @Override
    public synchronized void provides(IContext context, ITicket ticket, Set<String> toClone, IMoleExecution moleExecution) throws InternalProcessingError, UserBadDataError {
        //IMoleExecution execution = context.getGlobalValue(IMole.WorkflowExecution);
        ILevelComputing levelComputing = LevelComputing.getLevelComputing(moleExecution);

        int startLevel = levelComputing.getLevel(getStart());
        int endLevel = levelComputing.getLevel(getEnd());

        ITicket workingOnTicket = ticket;

        boolean toArray = endLevel < startLevel;

        //If from higher level
        for (int i = startLevel; i > endLevel; i--) {
            workingOnTicket = workingOnTicket.getParent();
            if (workingOnTicket.isRoot()) {
                Logger.getLogger(DataChannel.class.getName()).log(Level.SEVERE, "Bug should never get to root.");
            }
        }

        IRegistryWithTicket<IDataChannel, IContext> dataChannelRegistry = moleExecution.getLocalCommunication().getDataChannelRegistry();


        synchronized (dataChannelRegistry) {

            IContext curentContext = dataChannelRegistry.consult(this, workingOnTicket);

            if (curentContext == null) {
                curentContext = new Context();
                dataChannelRegistry.register(this, workingOnTicket, curentContext);
            }

            if (!toArray) {
                for (IData<?> data : getData()) {
                    if (context.containsVariableWithName(data.getPrototype().getName())) {
                        IVariable var = context.getLocalVariable(data.getPrototype());
                        IVariable tmp;

                        if (toClone.contains(data.getPrototype().getName())) {
                            tmp = new Variable(var.getPrototype(), Activator.getClonningService().clone(var.getValue()));
                        } else {
                            tmp = var;
                        }

                        curentContext.putVariable(tmp);
                    }
                }
            } else {
                for (IData<?> data : getData()) {
                    if (context.containsVariableWithName(data.getPrototype().getName())) {
                        IVariable var = context.getLocalVariable(data.getPrototype());
                        Collection curVal;
                        if(curentContext.containsVariableWithName(data.getPrototype())) {
                            curVal = curentContext.getLocalValue(data.getPrototype().array());
                        } else {
                            curVal = new ArrayList();
                            curentContext.putVariable(new Variable(data.getPrototype().array(), curVal));
                        }
                        curVal.add(var.getValue());
                    }
                }
            }


         /*   for (IData<?> data : getData()) {
                if (context.containsVariableWithName(data.getPrototype().getName())) {
                    IVariable var = context.getLocalVariable(data.getPrototype());

                    if (!curentContext.containsVariableWithName(data.getPrototype().getName())) {
                        IVariable tmp;

                        if (toClone.contains(data.getPrototype().getName())) {
                            tmp = new Variable(var.getPrototype(), Activator.getClonningService().clone(var.getValue()));
                        } else {
                            tmp = var;
                        }

                        if (toArray) {
                            List array = new ArrayList();
                            array.add(tmp);
                            tmp = new Variable(var.getPrototype().array(), array);
                        }

                        curentContext.putVariable(tmp);
                    } else {
                        IVariable currentVar = curentContext.getLocalVariable(data.getPrototype());

                        if (List.class.isAssignableFrom(currentVar.getPrototype().getType())) {
                            List curVal = (List) currentVar.getValue();
                            curVal.add(var.getValue());
                        } else {
                            Object curVal = currentVar.getValue();
                            List newVal = new ArrayList();
                            newVal.add(curVal);
                            IVariable tmp = new Variable(var.getPrototype().array(), newVal);
                            curentContext.putVariable(tmp);
                        }
                    }
                }
            }*/

            /*  } else {
            for (IData<?> data : getData()) {
            if (context.containsVariableWithName(data.getPrototype().getName())) {
            List currentVar = curentContext.<List>getLocalValue(data.getPrototype().getName());
            IVariable<?> var = context.getLocalVariable(data.getPrototype());
            IVariable tmp;

            if (toClone.contains(data.getPrototype().getName())) {
            tmp = new Variable(var.getPrototype(), Activator.getClonningService().clone(var.getValue()));
            } else {
            tmp = var;
            }
            currentVar.add(tmp);
            }
            }
            }*/

        }
    }

    @Override
    public void add(IPrototype prototype) {
        add(prototype.getName());
    }

    @ChangeState
    @Override
    public void add(String name) {
        variblesNames.add(name);
    }

    @Override
    public Set<String> getVariableNames() {
        return variblesNames;
    }

    @SoftCachable
    @Override
    public Iterable<IData> getData() throws InternalProcessingError, UserBadDataError {
        Collection<IData> ret = new LinkedList<IData>();

        for (IData<?> data : getEnd().getAssignedTask().getInput()) {
            if (variblesNames.contains(data.getPrototype().getName())) {
                ret.add(data);
            }
        }

        return ret;
    }

    @ChangeState
    @Override
    public void setStart(IGenericTaskCapsule start) {
        this.start = start;
    }

    @Override
    public void setEnd(IGenericTaskCapsule end) {
        this.end = end;
    }
}
