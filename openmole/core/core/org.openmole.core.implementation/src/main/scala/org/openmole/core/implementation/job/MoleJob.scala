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

import org.openmole.core.model.data._
import org.openmole.core.model.job._
import org.openmole.core.model.job.State._
import org.openmole.core.model.task._
import org.openmole.misc.tools.service.Logger
import java.util.UUID

object MoleJob extends Logger {
  type StateChangedCallBack = (IMoleJob, State, State) ⇒ Unit
  def apply(
    task: ITask,
    context: Context,
    id: UUID,
    stateChangedCallBack: MoleJob.StateChangedCallBack) = {
    val (prototypes, values) = compressContext(context)
    new MoleJob(task, prototypes.toArray, values.toArray, id.getMostSignificantBits, id.getLeastSignificantBits, stateChangedCallBack)
  }
  def compressContext(context: Context) =
    context.toSeq.map {
      case (_, v: Variable[Any]) ⇒ (v.prototype, v.value)
    }.unzip
}

class MoleJob(
    val task: ITask,
    private var prototypes: Array[Prototype[Any]],
    private var values: Array[Any],
    mostSignificantBits: Long, leastSignificantBits: Long,
    val stateChangedCallBack: MoleJob.StateChangedCallBack) extends IMoleJob {

  var exception: Option[Throwable] = None

  @volatile private var _state: State = null
  state = READY

  override def state: State = _state
  override def context: Context =
    Context((prototypes zip values).map { case (p, v) ⇒ Variable(p, v) })

  private def context_=(ctx: Context) = {
    val (_prototypes, _values) = MoleJob.compressContext(ctx)
    prototypes = _prototypes.toArray
    values = _values.toArray
  }

  def id = new UUID(mostSignificantBits, leastSignificantBits)

  def state_=(state: State) = {
    val changed = synchronized {
      if (_state == null) {
        _state = state
        None
      }
      else if (!_state.isFinal) {
        val oldState = _state
        _state = state
        Some(oldState)
      }
      else None
    }

    changed match {
      case Some(oldState) ⇒ stateChangedCallBack(this, oldState, state)
      case _              ⇒
    }
  }

  override def perform =
    if (!state.isFinal) {
      try {
        state = RUNNING
        context = task.perform(context)
        state = COMPLETED
      }
      catch {
        case t: Throwable ⇒
          exception = Some(t)
          state = FAILED
          if (classOf[InterruptedException].isAssignableFrom(t.getClass)) throw t
      }
    }

  override def finish(_context: Context) = {
    context = _context
    state = COMPLETED
  }

  override def finished: Boolean = state.isFinal

  override def cancel = state = CANCELED

}
