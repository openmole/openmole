/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.execution.IExecutionJob;
import org.openmole.core.model.job.IJob;

public abstract class ExecutionJob<ENV extends IEnvironment<?>>  implements IExecutionJob<ENV> {

    ENV executionEnvironment;
    IJob job;
    Long creationTime;

    public ExecutionJob(ENV executionEnvironment, IJob job) {
        super();
        this.executionEnvironment = executionEnvironment;
        this.job = job;
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    public ENV getEnvironment() {
        return executionEnvironment;
    }

    @Override
    public IJob getJob() {
        return job;
    }

    @Override
    public Long getCreationTime() {
        return creationTime;
    }
}
