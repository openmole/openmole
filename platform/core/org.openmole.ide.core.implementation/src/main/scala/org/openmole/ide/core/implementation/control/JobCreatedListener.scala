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
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.IMoleExecution.IOneJobSubmitted
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.tools.service.Priority

class JobCreatedListener extends IOneJobSubmitted{
  override def oneJobSubmitted(execution: IMoleExecution, moleJob: IMoleJob) = {
    EventDispatcher.registerForObjectChanged(execution,Priority.NORMAL,new JobSatusListener,IMoleExecution.OneJobStatusChanged)
  }
}

