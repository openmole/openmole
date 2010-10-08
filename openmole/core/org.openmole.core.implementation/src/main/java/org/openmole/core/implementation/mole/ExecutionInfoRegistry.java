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

package org.openmole.core.implementation.mole;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.core.model.job.IMoleJob;
import org.openmole.core.model.mole.IMoleExecution;

/**
 *
 * @author reuillon
 */
public class ExecutionInfoRegistry {
    final private static ExecutionInfoRegistry instance = new ExecutionInfoRegistry();

    final private Map<IMoleJob, IMoleExecution> registred = Collections.synchronizedMap(new WeakHashMap<IMoleJob, IMoleExecution>());

    private ExecutionInfoRegistry(){};

    public static ExecutionInfoRegistry GetInstance() {
        return instance;
    }

    public void register(IMoleJob job, IMoleExecution scheduler) {
        registred.put(job, scheduler);
    }

    public IMoleExecution getMoleExecution(IMoleJob job) {
        return registred.get(job);
    }
    
    public IMoleExecution remove(IMoleJob job) {
        return registred.remove(job);
    }
}
