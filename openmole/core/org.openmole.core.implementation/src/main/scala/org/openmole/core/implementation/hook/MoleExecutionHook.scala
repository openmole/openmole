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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.hook

import org.openmole.core.model.hook.IMoleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.job.State.State
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.mole.ICapsule
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.tools.service.Priority
import scala.ref.WeakReference

class MoleExecutionHook(private val moleExecution: WeakReference[IMoleExecution]) extends IMoleExecutionHook {

  def this(moleExecution: IMoleExecution) = this(new WeakReference(moleExecution))

  import Priority._
  import IMoleExecution._
  import EventDispatcher._

  resume

  override def resume = {
    listen(moleExecution(), HIGH, moleExecutionListener, classOf[Starting])
    listen(moleExecution(), HIGH, moleExecutionListener, classOf[OneJobStatusChanged])
    listen(moleExecution(), LOW, moleExecutionListener, classOf[Finished])
    listen(moleExecution(), NORMAL, moleExecutionListener, classOf[JobInCapsuleFinished])
    listen(moleExecution(), NORMAL, moleExecutionListener, classOf[JobInCapsuleStarting])
  }

  override def release = {
    unlisten(moleExecution(), moleExecutionListener, classOf[Starting])
    unlisten(moleExecution(), moleExecutionListener, classOf[OneJobStatusChanged])
    unlisten(moleExecution(), moleExecutionListener, classOf[Finished])
    unlisten(moleExecution(), moleExecutionListener, classOf[JobInCapsuleFinished])
    unlisten(moleExecution(), moleExecutionListener, classOf[JobInCapsuleStarting])
  }

  override def jobFinished(moleJob: IMoleJob, capsule: ICapsule) = {}
  override def jobStarting(moleJob: IMoleJob, capsule: ICapsule) = {}

  override def stateChanged(moleJob: IMoleJob, newState: State, oldState: State) = {}
  override def executionStarting = {}
  override def executionFinished = {}

  @transient lazy val moleExecutionListener = new EventListener[IMoleExecution] {
    override def triggered(obj: IMoleExecution, ev: Event[IMoleExecution]) =
      ev match {
        case ev: OneJobStatusChanged ⇒ stateChanged(ev.moleJob, ev.newState, ev.oldState)
        case ev: JobInCapsuleStarting ⇒ jobStarting(ev.moleJob, ev.capsule)
        case ev: JobInCapsuleFinished ⇒ jobFinished(ev.moleJob, ev.capsule)
        case ev: Starting ⇒ executionStarting
        case ev: Finished ⇒ executionFinished
        case _ ⇒
      }
  }

}
