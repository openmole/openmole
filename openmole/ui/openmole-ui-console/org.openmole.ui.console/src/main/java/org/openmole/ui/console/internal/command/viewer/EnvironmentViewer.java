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
package org.openmole.ui.console.internal.command.viewer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.openmole.core.model.execution.ExecutionState;
import org.openmole.core.model.execution.IEnvironment;
import org.openmole.core.model.execution.IExecutionJob;
import org.openmole.core.model.execution.IExecutionJobRegistry;

import scala.collection.Iterator;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class EnvironmentViewer implements IViewer<IEnvironment> {

    @Override
    public void view(IEnvironment object, String[] args) {
        Map<ExecutionState, AtomicInteger> accounting = new EnumMap<ExecutionState, AtomicInteger>(ExecutionState.class);

        for (ExecutionState state : ExecutionState.values()) {
            accounting.put(state, new AtomicInteger());
        }

        Iterator<IExecutionJob> it = object.jobRegistry().getAllExecutionJobs().iterator();

        while (it.hasNext()) {
            IExecutionJob executionJob = it.next();
            accounting.get(executionJob.state()).incrementAndGet();
        }

        for (ExecutionState state : ExecutionState.values()) {
            System.out.println(state.name() + ": " + accounting.get(state));
        }
    }
}
