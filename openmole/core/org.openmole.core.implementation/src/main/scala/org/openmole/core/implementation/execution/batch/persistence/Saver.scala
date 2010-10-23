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

package org.openmole.core.implementation.execution.batch.persistence

import java.io.File
import java.io.IOException
import java.util.TreeSet
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.implementation.job.Context
import org.openmole.core.implementation.observer.IMoleExecutionObserver
import org.openmole.core.implementation.observer.MoleExecutionObserverAdapter
import org.openmole.core.model.capsule.IGenericTaskCapsule
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.commons.tools.io.FileUtil._
import org.openmole.core.model.persistence.IPersistentContext._

import scala.collection.JavaConversions._

class Saver(moleExection: IMoleExecution, taskCapsule: IGenericTaskCapsule[_,_], dir: File) extends IMoleExecutionObserver {

  new MoleExecutionObserverAdapter(moleExection, this)
  dir.mkdirs
  new File(dir, FILES).mkdir
  new File(dir, CONTEXTS).mkdir
  new File(dir, CONTEXTS_LINK).mkdir

  def this(moleExection: IMoleExecution, taskCapsule: IGenericTaskCapsule[_,_], dir: String) = {
    this(moleExection, taskCapsule, new File(dir));
  }


  override def moleJobFinished(moleJob: IMoleJob) = {
    synchronized {
      val filter = new TreeSet[String]
        
      for(data <- moleJob.getTask.getOutput) {
        if(data.getMode().isSystem()) filter.add(data.getPrototype().getName());
      }
        
      val context = new Context
        
      for(variable <- moleJob.getContext) {
        if(!filter.contains(variable.getPrototype().getName())) context.putVariable(variable);
      }

      val serialization = Activator.getSerializer.serializeFilePathAsHashGetPluginClassAndFiles(context, new File(dir, CONTEXTS))

      try {
        val files = new File(dir, FILES)
        for (f <- serialization._1) {         
          val file = new File(files, f._2.fileHash.toString)
          if (!file.exists) {
            copy(f._1, file);
          }
        }
      } catch {
        case(e: IOException) => throw new InternalProcessingError(e)
      }
    }
  }

  override def moleExecutionStarting = {
    if (!dir.exists) {
      dir.mkdirs
    }
  }

  override def moleExecutionFinished = {}
}
