/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.implementation.observer

import org.openmole.commons.aspect.eventdispatcher.IObjectListener
import org.openmole.commons.aspect.eventdispatcher.IObjectListenerWithArgs
import org.openmole.commons.tools.service.Priority
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution


class MoleExecutionObserverAdapter private (moleExecutionObserver: IMoleExecutionObserver) {
    
  def this(moleExecution: IMoleExecution, moleExecutionObserver: IMoleExecutionObserver) = {
    this(moleExecutionObserver)
    Activator.getEventDispatcher.registerForObjectChangedSynchronous(moleExecution, Priority.HIGH, new MoleExecutionExecutionStartingAdapter, IMoleExecution.Starting)
    Activator.getEventDispatcher.registerForObjectChangedSynchronous(moleExecution, Priority.HIGH, new MoleExecutionOneJobFinishedAdapter, IMoleExecution.OneJobFinished)
    Activator.getEventDispatcher.registerForObjectChangedSynchronous(moleExecution, Priority.LOW, new MoleExecutionExecutionFinishedAdapter, IMoleExecution.Finished)
  }
  
  
  class MoleExecutionOneJobFinishedAdapter extends IObjectListenerWithArgs[IMoleExecution]  {

    override def eventOccured(obj: IMoleExecution, args: Array[Object]) = {
      moleExecutionObserver.moleJobFinished(args(0).asInstanceOf[IMoleJob])
    }

  }

  class MoleExecutionExecutionStartingAdapter extends IObjectListener[IMoleExecution]  {

    override def eventOccured(obj: IMoleExecution) = {
      moleExecutionObserver.moleExecutionStarting
    }
  }

  class MoleExecutionExecutionFinishedAdapter extends IObjectListener[IMoleExecution]  {

    override def eventOccured(obj: IMoleExecution) = {
      moleExecutionObserver.moleExecutionFinished
    }
  }
   
}
