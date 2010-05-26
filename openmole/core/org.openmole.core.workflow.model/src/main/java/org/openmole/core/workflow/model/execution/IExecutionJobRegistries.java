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

package org.openmole.core.workflow.model.execution;

import java.util.Collection;
import org.openmole.core.workflow.model.job.IJob;
import org.openmole.core.workflow.model.mole.IExecutionContext;


import org.openmole.commons.tools.structure.Trio;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IExecutionJobRegistries<EXECUTIONJOB extends IExecutionJob> {
    void register(IExecutionContext context, IJobStatisticCategory capsule, EXECUTIONJOB executionJob);
    IExecutionContext getExecutionContext(EXECUTIONJOB job);
    IJobStatisticCategory getJobStatisticCategory(EXECUTIONJOB job);
    Collection<EXECUTIONJOB> getAllExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory, IJob job);
    Collection<EXECUTIONJOB> getAllExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory);
    Collection<EXECUTIONJOB> getAllLastExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory);
    Integer getNbExecutionJobs(IExecutionContext context, IJobStatisticCategory statisticCategory, IJob job);
    Iterable<Trio<IExecutionContext,IJobStatisticCategory,IJob>> getAllJobs();
    void remove(IExecutionContext context, IJobStatisticCategory statisticCategory, IJob job);
    void remove(IExecutionContext context, IJobStatisticCategory statisticCategory, EXECUTIONJOB executionJob);
    EXECUTIONJOB findLastExecutionJob(IExecutionContext context, IJobStatisticCategory jobStatisticCategory, IJob job);
    Iterable<EXECUTIONJOB> allExecutionjobs();
    Collection<IJob> getAllJobs(IExecutionContext context, IJobStatisticCategory statisticCategory);

}
