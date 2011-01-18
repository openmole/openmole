/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.console.internal.command.viewer

import java.util.concurrent.atomic.AtomicInteger
import org.openmole.core.model.execution.ExecutionState
import org.openmole.core.model.execution.IEnvironment

class EnvironmentViewer extends IViewer[IEnvironment]{

  override def view(obj: IEnvironment, args: Array[String]) {
    val accounting = new Array[AtomicInteger](ExecutionState.values.size)
    //Map<ExecutionState.ExecutionState, AtomicInteger> accounting = new EnumMap<ExecutionState.ExecutionState, AtomicInteger>(ExecutionState.ExecutionState.class);

    for (state <- ExecutionState.values) {
      accounting(state.id) = new AtomicInteger
    }

    for (executionJob <- obj.jobRegistry.allExecutionJobs) {
      accounting(executionJob.state.id).incrementAndGet
    }

    for (state <- ExecutionState.values) {
      System.out.println(state.toString + ": " + accounting(state.id))
    }
  }
}
