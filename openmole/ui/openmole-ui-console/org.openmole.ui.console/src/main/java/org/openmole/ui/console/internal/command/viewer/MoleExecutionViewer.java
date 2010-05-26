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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.openmole.core.workflow.model.job.IMoleJob;
import org.openmole.core.workflow.model.job.State;
import org.openmole.core.workflow.model.mole.IMoleExecution;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class MoleExecutionViewer implements IViewer<IMoleExecution>{

    @Override
    public void view(IMoleExecution object, List<Object> args) {
        Map<State,AtomicInteger> toDisplay = new TreeMap<State, AtomicInteger>();

        for(State state : State.values()) {
            toDisplay.put(state, new AtomicInteger());
        }

        for(IMoleJob job : object.getAllMoleJobs()) {
            toDisplay.get(job.getState()).incrementAndGet();
        }

        for(State state : State.values()) {
            System.out.println(state.getLabel() + ": " + toDisplay.get(state));
        }
    }

}
