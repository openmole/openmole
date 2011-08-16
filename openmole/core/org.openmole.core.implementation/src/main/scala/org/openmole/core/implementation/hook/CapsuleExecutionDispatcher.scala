/*
 * Copyright (C) 2011 reuillon
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

import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.HashSet
import scala.collection.mutable.WeakHashMap
import scala.ref.WeakReference

object CapsuleExecutionDispatcher extends Logger {
  
  private val dispatchers = new WeakHashMap[IMoleExecution, DispatcherMoleExecutionHook]
  
  class DispatcherMoleExecutionHook(moleExecution: WeakReference[IMoleExecution]) extends MoleExecutionHook(moleExecution) {
    
    def this(moleExecution: IMoleExecution) = this(new WeakReference(moleExecution))
  
    private val hub = new WeakHashMap[ICapsule, HashSet[CapsuleExecutionHook]]
    
    override def jobInCapsuleFinished(moleJob: IMoleJob, capsule: ICapsule) = hub.synchronized {
      hub.getOrElse(capsule, Iterable.empty).foreach(_.safeProcess(moleJob))
    }
    
    def +=(capsule: ICapsule, hook: CapsuleExecutionHook) = hub.synchronized {
      hub.getOrElseUpdate(capsule, new HashSet) += hook
    }
    
    def -=(capsule: ICapsule, hook: CapsuleExecutionHook) = hub.synchronized {
      hub.getOrElse(capsule, HashSet.empty) -= hook
    }
    
  }
  
  def +=(execution: IMoleExecution, capsule: ICapsule, hook: CapsuleExecutionHook) =  dispatchers.synchronized {
    dispatchers.getOrElseUpdate(execution, new DispatcherMoleExecutionHook(execution)) += (capsule, hook)
  }
  
  def -=(execution: IMoleExecution, capsule: ICapsule, hook: CapsuleExecutionHook) =  dispatchers.synchronized {
    dispatchers.get(execution) match {
      case Some(dispatcher) => dispatcher -= (capsule, hook)
      case None => 
    }
  }

}
