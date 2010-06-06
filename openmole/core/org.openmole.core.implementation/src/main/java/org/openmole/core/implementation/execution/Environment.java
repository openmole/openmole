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
package org.openmole.core.implementation.execution;

import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.execution.IEnvironmentExecutionStatistics;
import org.openmole.core.model.execution.IExecutionJob;
import org.openmole.core.model.execution.IExecutionJobRegistries;
import org.openmole.core.model.execution.IJobStatisticCategory;
import org.openmole.core.model.execution.batch.SampleType;
import org.openmole.core.model.mole.IExecutionContext;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.workspace.ConfigurationLocation;

public abstract class Environment<EXECUTIONJOB extends IExecutionJob> implements IEnvironment<EXECUTIONJOB> {

    final static String ConfigGroup = Environment.class.getSimpleName();
    final static ConfigurationLocation StatisticsHistorySize = new ConfigurationLocation(ConfigGroup, "StatisticsHistorySize");

    static {
            Activator.getWorkspace().addToConfigurations(StatisticsHistorySize, "1000");
    }

    IEnvironmentExecutionStatistics statistics;
    IExecutionJobRegistries<EXECUTIONJOB> jobRegistries = new ExecutionJobRegistries<EXECUTIONJOB>();

    public Environment() throws InternalProcessingError {
        super();
        statistics = new EnvironmentExecutionStatistics(Activator.getWorkspace().getPreferenceAsInt(StatisticsHistorySize));
    }

    @Override
    public IExecutionJobRegistries<EXECUTIONJOB> getJobRegistries() {
        return jobRegistries;
    }

    @Override
    public IEnvironmentExecutionStatistics getStatistics() {
        return statistics;
    }

    public void sample(SampleType type, Long value, IExecutionContext executionContext, IJobStatisticCategory statisticCategory) {
         getStatistics().statusJustChanged(type, value, executionContext, statisticCategory);
    }
}
