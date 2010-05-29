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

import java.util.Set;
import java.util.TreeSet;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.transition.ITransition;
import org.openmole.core.model.data.IDataChannel;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author reuillon
 */
public class ToCloneFinder {


    //TODO Improvement condition are evaluated several times
    public static Set<String> getVariablesToClone(IGenericTaskCapsule<?, ?> caps, IContext context) throws InternalProcessingError, UserBadDataError {
        Iterable<? extends ITransition> outputTransitions = caps.getOutputTransitions();

        Set<String> allReadySeen = new TreeSet<String>();
        Set<String> ret = new TreeSet<String>();

        for (ITransition transition : outputTransitions) {
            if (transition.isConditionTrue(context)) {
                for (IData<?> data : transition.getEnd().getCapsule().getTask().getInput()) {
                    String name = data.getPrototype().getName();

                    if (allReadySeen.contains(name)) {
                        ret.add(name);
                    } else {
                        allReadySeen.add(name);
                    }
                }
            }
        }

        for (IDataChannel channel : caps.getOutputDataChannels()) {
            for (IData<?> data : channel.getData()) {
                String name = data.getPrototype().getName();

                if (allReadySeen.contains(name)) {
                    ret.add(name);
                } else {
                    allReadySeen.add(name);
                }
            }
        }

        return ret;
    }
}
