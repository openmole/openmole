/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.core.implementation.execution;

import java.util.Arrays;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.job.IJob;
import org.openmole.core.model.job.IMoleJob;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class JobStatisticCategory implements IJobStatisticCategory {

    final Object[] fingerPrint;

    public JobStatisticCategory(IJob job) {
        fingerPrint = new Object[job.size() + 1];
        int i = 0;

        for (IMoleJob moleJob : job.getMoleJobs()) {
            fingerPrint[i++] = moleJob.getTask();
        }

        fingerPrint[i++] = JobRegistry.getInstance().getMoleExecutionForJob(job);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(fingerPrint);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JobStatisticCategory other = (JobStatisticCategory) obj;
        if (!Arrays.deepEquals(this.fingerPrint, other.fingerPrint)) {
            return false;
        }
        return true;
    }

}
