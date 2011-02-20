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

package org.openmole.core.implementation.persistence

import java.io.File
import java.io.IOException
import java.util.TreeSet
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.observer.IMoleExecutionObserver
import org.openmole.core.implementation.observer.MoleExecutionObserverAdapter
import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.commons.tools.io.FileUtil._
import org.openmole.core.model.persistence.PersistentContext._
import org.openmole.core.serializer.Serializer
import scala.collection.JavaConversions
import scala.collection.JavaConversions._

class Saver private (taskCapsule: IGenericCapsule, dir: File) extends IMoleExecutionObserver {

   var ordinal: Int = 0
  
  def this(moleExection: IMoleExecution, taskCapsule: IGenericCapsule, dir: File) = {
    this(taskCapsule, dir)
     new MoleExecutionObserverAdapter(moleExection, this)
  }

  def this(moleExection: IMoleExecution, taskCapsule: IGenericCapsule, dir: String) = {
    this(moleExection, taskCapsule, new File(dir));
  }


  override def moleJobFinished(moleJob: IMoleJob) = {
    synchronized {
      val filter = new TreeSet[String]
      
      for(data <- moleJob.task.userOutputs) {
        filter.add(data.prototype.name)
      }
        
      val context = new Context
       
      for(variable <- moleJob.context) {
        if(!filter.contains(variable.prototype.name)) context += variable
      }

      val serialization = Serializer.serializeFilePathAsHashGetPluginClassAndFiles(context, new File(dir, CONTEXT))

      val ctxLink = new File(dir, CONTEXT_LINK)
      val link = new File(ctxLink,  serialization._3 + SEPARATOR + ordinal.toString)
      link.createNewFile
      
      ordinal += 1
      
      try {
        val files = new File(dir, FILE)
        for (f <- serialization._1) {         
          val file = new File(files, f._2.fileHash.toString)
          if (!file.exists) {
            copy(f._1, file)
          }
        }
      } catch {
        case(e: IOException) => throw new InternalProcessingError(e)
      }
    }
  }

  override def moleExecutionStarting = {
      dir.mkdirs
      new File(dir, FILE).mkdir
      new File(dir, CONTEXT).mkdir
      new File(dir, CONTEXT_LINK).mkdir
      ordinal = 0
  }

  override def moleExecutionFinished = {}
}
