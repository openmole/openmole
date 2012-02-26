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

package org.openmole.core.implementation.hook

import org.openmole.core.model.execution.ExecutionState._
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.execution.IExecutionJob
import org.openmole.core.model.hook.IEnvironmentHook
import org.openmole.misc.eventdispatcher.Event
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.eventdispatcher.EventListener
import org.openmole.misc.tools.service.Priority
import scala.ref.WeakReference


class EnvironmentHook(private val environment: WeakReference[IEnvironment]) extends IEnvironmentHook {
  
  def this(environment: IEnvironment) = this(new WeakReference(environment))
  
  import Priority._
  import EventDispatcher._
  import IEnvironment._
  
  resume
  
  override def resume = {
    listen(environment(), HIGH, environmentListener, classOf[JobStateChanged])
  }
  
  override def release = {
    unlisten(environment(), environmentListener, classOf[JobStateChanged])
  }
  
  val environmentListener = new EventListener[IEnvironment] {
    override def triggered(obj: IEnvironment, ev: Event[IEnvironment]) = 
      ev match {
        case ev: JobStateChanged => jobStatusChanged(ev.job, ev.newState, ev.oldState)
        case _ =>
      }
    
  }
  
}
