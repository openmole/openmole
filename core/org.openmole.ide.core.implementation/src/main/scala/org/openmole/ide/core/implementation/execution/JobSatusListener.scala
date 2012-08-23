/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.execution

import org.openmole.core.model.job.State._
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.core.model.mole.IMoleExecution._

case object MoleFinishedEvent extends scala.swing.event.Event

class JobSatusListener(exeManager: ExecutionManager) extends EventListener[IMoleExecution] {
  override def triggered(execution: IMoleExecution, event: Event[IMoleExecution]) = {
    event match {
      case x: OneJobStatusChanged ⇒
        exeManager.wfPiePlotter.update(x.oldState, exeManager.status(x.oldState).decrementAndGet)
        exeManager.wfPiePlotter.update(x.newState, exeManager.status(x.newState).incrementAndGet)
      case x: Finished ⇒
        exeManager.logTextArea.append("The simulation is completed")
        exeManager.publish(MoleFinishedEvent)
    }
  }
}