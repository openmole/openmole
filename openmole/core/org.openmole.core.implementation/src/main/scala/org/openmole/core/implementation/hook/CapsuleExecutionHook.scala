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

import java.util.logging.Level
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.hook.ICapsuleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.tools.service.Logger
import scala.ref.WeakReference

abstract class CapsuleExecutionHook(moleExecutionRef: WeakReference[IMoleExecution], capsuleRef: WeakReference[ICapsule]) extends ICapsuleExecutionHook with Logger {
  
  def this(moleExecution: IMoleExecution, capsule: ICapsule) = this(new WeakReference(moleExecution), new WeakReference(capsule))
  
  resume
  
  private def moleExecution = moleExecutionRef.get.getOrElse(throw new InternalProcessingError("Reference garabage collected."))
  private def capsule = capsuleRef.get.getOrElse(throw new InternalProcessingError("Reference garabage collected."))
  
  override def resume = CapsuleExecutionDispatcher += (moleExecution, capsule, this)
  override def release = CapsuleExecutionDispatcher -= (moleExecution, capsule, this)
  
  def safeProcess(moleJob: IMoleJob) = 
    try process(moleJob) 
    catch { case e => logger.log(Level.SEVERE,"Error durring hook execution", e)}
    
  def process(moleJob: IMoleJob)
}
