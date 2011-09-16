/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.control

import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State.State
import org.openmole.core.model.job.State._
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IMoleExecution.IOneJobStatusChanged
import org.openmole.ide.misc.exception.GUIUserBadDataError

class JobSatusListener extends IOneJobStatusChanged {
  override def oneJobStatusChanged(execution: IMoleExecution, moleJob: IMoleJob, newState: State, oldState: State) = {
    val exeManager = TabManager.executionManager(execution)
    exeManager.status(oldState) -= 1 
    exeManager.status(newState) += 1 
    exeManager.wfPiePlotter.updateData(oldState.name,exeManager.status(oldState))
    exeManager.wfPiePlotter.updateData(newState.name,exeManager.status(newState))}
}