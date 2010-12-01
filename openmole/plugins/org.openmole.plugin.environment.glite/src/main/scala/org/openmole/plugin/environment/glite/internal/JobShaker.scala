/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.plugin.environment.glite.internal

import org.openmole.commons.tools.service.RNG
import org.openmole.core.model.execution.ExecutionState._
import org.openmole.misc.updater.IUpdatable
import org.openmole.plugin.environment.glite.GliteEnvironment
import scala.ref.WeakReference

class JobShaker(environment: WeakReference[GliteEnvironment], shakingProbability: Double) extends IUpdatable {
 
  def this(environment: GliteEnvironment, shakingProbability: Double) = this(new WeakReference(environment), shakingProbability)
  
  override def update: Boolean = {
    val env = environment.get match {
      case None => return false
      case Some(env) => env
    }
    val registry = env.jobRegistry
    registry.synchronized {
      for (job <- registry.allExecutionJobs) {
        if(job.state == SUBMITTED && RNG.nextDouble < shakingProbability) {
          job.kill
          env.submit(job.job)
        }
      }   
    }
    true
 }

}
