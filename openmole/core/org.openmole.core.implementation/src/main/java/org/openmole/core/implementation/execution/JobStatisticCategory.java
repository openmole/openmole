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

import org.apache.commons.collections.keyvalue.MultiKey;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class JobStatisticCategory implements IJobStatisticCategory {

    final MultiKey key;

    public JobStatisticCategory(IJob job) {
        Object[] tasks = new Object[job.size() + 1];
        int i = 0;

        for (IMoleJob moleJob : job.getMoleJobs()) {
            tasks[i++] = moleJob.getTask();
        }

        tasks[i++] = JobRegistry.getInstance().getMoleExecutionForJob(job);

        this.key = new MultiKey(tasks);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return key.equals(other);
    }
}
