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
package org.openmole.core.implementation.tools;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.model.transition.IGenericTransition;
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
    public static Set<String> getVariablesToClone(IGenericTaskCapsule<?, ?> caps, IContext global, IContext context) throws InternalProcessingError, UserBadDataError {

        class DataInfo {
            AtomicInteger nbUsage = new AtomicInteger();
            AtomicInteger nbMutable = new AtomicInteger();
        }

        Iterable<? extends IGenericTransition> outputTransitions = caps.getOutputTransitions();

        Map<String, DataInfo> counters = new TreeMap<String, DataInfo>();

        for (IGenericTransition transition : outputTransitions) {
            if (transition.isConditionTrue(global, context)) {
                for (IData<?> data : transition.getEnd().getCapsule().getTask().getInput()) {
                    String name = data.getPrototype().getName();

                    DataInfo info = counters.get(name);

                    if (info == null) {
                        info = new DataInfo();
                        counters.put(name, info);
                    }

                    info.nbUsage.incrementAndGet();
                    if (!data.getMode().isImmutable()) {
                        info.nbMutable.incrementAndGet();
                    }
                }
            }
        }

        for (IDataChannel channel : caps.getOutputDataChannels()) {
            for (IData<?> data : channel.getData()) {
                String name = data.getPrototype().getName();

                DataInfo info = counters.get(name);

                if (info == null) {
                    info = new DataInfo();
                    counters.put(name, info);
                }

                info.nbUsage.incrementAndGet();
                if (!data.getMode().isImmutable()) {
                    info.nbMutable.incrementAndGet();
                }
            }
        }

        Set<String> toClone = new TreeSet<String>();

        for(Map.Entry<String, DataInfo> entry: counters.entrySet()) {
            if(entry.getValue().nbUsage.get() > 1 && entry.getValue().nbMutable.get() > 0) {
                toClone.add(entry.getKey());
            }
        }

        return toClone;
    }
}
