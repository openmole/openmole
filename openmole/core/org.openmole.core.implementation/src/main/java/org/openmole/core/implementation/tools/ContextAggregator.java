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

package org.openmole.core.implementation.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.DataSet;
import org.openmole.core.implementation.data.Variable;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IDataSet;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class ContextAggregator {


    public static IDataSet findDataIn1WhichAreAlsoIn2(IDataSet one, IDataSet two) {
        Set<String> beginVarSet = new TreeSet<String>();

        for (IData data : two) {
            beginVarSet.add(data.getPrototype().getName());
        }

        List<IData<?>> toAggregate = new LinkedList<IData<?>>();

        for (IData data : one) {
            if (beginVarSet.contains(data.getPrototype().getName())) {
                toAggregate.add(data);
            }
        }

        return new DataSet(toAggregate);
    }

    public static void aggregate(IContext inContext, IDataSet aggregate, Set<String> toClone, boolean forceArrays, Collection<IContext> toAgregate) throws InternalProcessingError, UserBadDataError {
        Set<String> mergingVars = new TreeSet<String>();

        for (IContext current : toAgregate) {
            for (IData<?> data : aggregate) {
                IPrototype<?> inProt = data.getPrototype();

                if ((!data.getMod().isOptional() || data.getMod().isOptional()) && current.containsVariableWithName(inProt.getName())) {
                    // if (current.exist(inProt.getName())) {
                    IVariable<?> tmp;

                    if (toClone.contains(inProt.getName())) {
                        IVariable<?> varToClone = current.getLocalVariable(inProt);
                        tmp = ClonningService.clone(varToClone);
                    } else {
                        tmp = current.getVariable(inProt.getName());
                    }

                    /* Var is allready present in context => merge */
                    if (forceArrays || inContext.containsVariableWithName(inProt.getName())) {
                        /* Var is being merged */
                        if (mergingVars.contains(tmp.getPrototype().getName())) {
                            inContext.<List>getLocalValue(tmp.getPrototype().getName()).add(tmp.getValue());
                        } else {
                            mergingVars.add(inProt.getName());
                            //Array list for non-recursive clonning
                            List agregationList = new ArrayList();
                            agregationList.add(tmp.getValue());
                            if (inContext.containsVariableWithName(inProt.getName())) {
                                agregationList.add(inContext.getLocalValue(inProt.getName()));
                            }
                            inContext.putVariable(inProt.getName(), List.class, agregationList);
                        }
                    } /* Just move the var in the new context (no collision)*/ else {
                        inContext.putVariable(tmp);
                    }

                }

            }

        }
    }


   
}
