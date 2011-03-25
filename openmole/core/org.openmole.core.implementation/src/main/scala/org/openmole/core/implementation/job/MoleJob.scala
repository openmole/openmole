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

package org.openmole.core.implementation.job

import java.util.concurrent.ExecutionException
import java.util.logging.Level
import java.util.logging.Logger
import org.openmole.core.implementation.execution.Progress
import org.openmole.core.implementation.task.GenericTask
import org.openmole.core.implementation.tools.LocalHostName
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.ITimeStamp
import org.openmole.core.model.job.State._
import org.openmole.core.model.job.State
import org.openmole.core.model.task.IGenericTask
import org.openmole.misc.eventdispatcher.EventDispatcher
import scala.collection.mutable.ArrayBuffer

object MoleJob {
  val LOGGER = Logger.getLogger(classOf[MoleJob].getName)
}

class MoleJob(val task: IGenericTask, private var _context: IContext, val id: MoleJobId) extends IMoleJob {
    
  val progress = new Progress
    
  @volatile  private var _state: State = null
  state = READY
    
  override def state: State = _state
  override def context: IContext = _context
    
  //@ObjectModified(name = IMoleJob.StateChanged)
  def state_=(state: State) = synchronized {
    if(_state == null || !_state.isFinal) {
      val timeStamps = context.value(GenericTask.Timestamps.prototype) match {
        case None => 
          val ret = new ArrayBuffer[ITimeStamp](5)
          context += (GenericTask.Timestamps.prototype, ret)
          ret
        case Some(ts) => ts
      }
     // MoleJob.LOGGER.info("Before " + task.name + " " + timeStamps.map{ _.state.toString } + " " + timeStamps.getClass)
      timeStamps += new TimeStamp(state, LocalHostName.localHostName, System.currentTimeMillis)
     // MoleJob.LOGGER.info("After " + task.name + " " + timeStamps.map{ _.state.toString } + timeStamps.getClass)
      _state = state
      EventDispatcher.objectChanged(this, IMoleJob.StateChanged, Array(state))
    }
  }


  override def perform = {
    try {
      state = RUNNING
      task.perform(context, progress)
    } catch {
      case e =>
        context += (GenericTask.Exception.prototype, e)

        if (classOf[InterruptedException].isAssignableFrom(e.getClass)) {
          throw e
        }
    }
  }

  override def rethrowException(context: IContext) = {
    context.value(GenericTask.Exception.prototype) match {
      case None =>
      case Some(e) => throw new ExecutionException("Error durring job execution for task " + task.name, e)
    }
  }

  override def finished(context: IContext) = {
    _context = context

    context.value(GenericTask.Exception.prototype) match {
      case None => state = COMPLETED
      case Some(ex) =>
        state = FAILED
        MoleJob.LOGGER.log(Level.SEVERE, "Error in user job execution, job state is FAILED.", ex)
    }
  }

  override def isFinished: Boolean = state.isFinal
    
  override def cancel = {
    state = CANCELED
  }

}
