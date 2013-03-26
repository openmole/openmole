/*
 * Copyright (C) 2010 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.job

import org.openmole.core.implementation.tools.TimeStamp
import org.openmole.core.model.tools.ITimeStamp
import org.openmole.core.model.data.Context
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.MoleJobId
import org.openmole.core.model.job.State._
import org.openmole.core.model.job.State
import org.openmole.core.model.task.ITask
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.ListBuffer

object MoleJob extends Logger {
  type StateChangedCallBack = (IMoleJob, State, State) ⇒ Unit

  def cpuTime(job: IMoleJob) =
    job.timeStamps.find(j ⇒ j.state.isFinal) match {
      case Some(last) ⇒
        val running = job.timeStamps.find(_.state == RUNNING).get
        last.time - running.time
      case None ⇒ 0L
    }

}

import MoleJob._

class MoleJob(
    val task: ITask,
    private var _context: Context,
    val id: MoleJobId,
    val stateChangedCallBack: MoleJob.StateChangedCallBack) extends IMoleJob {

  import IMoleJob._
  import MoleJob._

  val timeStamps: ListBuffer[ITimeStamp[State.State]] = new ListBuffer
  var exception: Option[Throwable] = None

  @volatile private var _state: State = null
  state = READY

  override def state: State = _state
  override def context: Context = _context

  def state_=(state: State) = {
    val changed = synchronized {
      if (_state == null) {
        timeStamps += new TimeStamp(state)
        _state = state
        None
      } else if (!_state.isFinal) {
        timeStamps += new TimeStamp(state)
        val oldState = _state
        _state = state
        Some(oldState)
      } else None
    }

    changed match {
      case Some(oldState) ⇒ stateChangedCallBack(this, oldState, state)
      case _ ⇒
    }
  }

  override def perform =
    if (!state.isFinal) {
      try {
        state = RUNNING
        _context = task.perform(context)
        state = COMPLETED
      } catch {
        case t: Throwable ⇒
          exception = Some(t)
          state = FAILED
          if (classOf[InterruptedException].isAssignableFrom(t.getClass)) throw t
      }
    }

  override def finish(context: Context, timeStamps: Seq[ITimeStamp[State.State]]) = {
    _context = context
    this.timeStamps ++= timeStamps
    state = COMPLETED
  }

  override def finished: Boolean = state.isFinal

  override def cancel = state = CANCELED

}
