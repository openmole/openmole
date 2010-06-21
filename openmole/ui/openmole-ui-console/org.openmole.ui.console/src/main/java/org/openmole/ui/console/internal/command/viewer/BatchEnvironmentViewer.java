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

package org.openmole.ui.console.internal.command.viewer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.IExecutionJobRegistry;
import org.openmole.core.model.execution.batch.IBatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchExecutionJob;
import org.openmole.core.model.execution.batch.IBatchJob;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class BatchEnvironmentViewer implements IViewer<IBatchEnvironment>{

    EnvironmentViewer environmentViewer = new EnvironmentViewer();

    @Override
    public void view(IBatchEnvironment object, List<Object> args) {
        environmentViewer.view(object, args);
        System.out.println(Separator);

        Map<IBatchServiceDescription, Map<ExecutionState, AtomicInteger>> jobServices = new HashMap<IBatchServiceDescription, Map<ExecutionState, AtomicInteger>>();
 
        IExecutionJobRegistry<IBatchExecutionJob> executionJobRegistry = object.getJobRegistry();

        for(IBatchExecutionJob executionJob: executionJobRegistry.getAllExecutionJobs()) {
            IBatchJob batchJob = executionJob.getBatchJob();
            if(batchJob != null) {
                Map<ExecutionState, AtomicInteger> jobServiceInfo = jobServices.get(batchJob.getBatchJobServiceDescription());
                if(jobServiceInfo == null ) {
                    jobServiceInfo = new TreeMap<ExecutionState, AtomicInteger>();
                    jobServices.put(batchJob.getBatchJobServiceDescription(), jobServiceInfo);
                }

                AtomicInteger nb = jobServiceInfo.get(batchJob.getState());
                if(nb == null) {
                    nb = new AtomicInteger();
                    jobServiceInfo.put(batchJob.getState(), nb);
                }
                nb.incrementAndGet();
            }
        }

        for(IBatchServiceDescription description : jobServices.keySet()) {
            System.out.print(description.toString() + ":");
            Map<ExecutionState, AtomicInteger> jobServiceInfo =jobServices.get(description);

            for(ExecutionState state: jobServiceInfo.keySet()) {
                System.out.print(" [" + state.getLabel() + " = " + jobServiceInfo.get(state).get() + "]");
            }
            System.out.println();
        }



    }

}
