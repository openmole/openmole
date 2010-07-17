/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.core.implementation.execution;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.mole.IMoleExecution;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class JobRegistry {

    private JobRegistry(){}

    static JobRegistry instance = new JobRegistry();

    Map<IJob, IMoleExecution> registry = Collections.synchronizedMap(new WeakHashMap<IJob, IMoleExecution>());

    public static JobRegistry getInstance() {
        return instance;
    }
    
    public void register(IJob job, IMoleExecution moleExecution) {
        registry.put(job, moleExecution);
    }
    
    public void remove(IJob job) {
        registry.remove(job);
    }

    public IMoleExecution getMoleExecutionForJob(IJob job) {
        return registry.get(job);
    }
}
