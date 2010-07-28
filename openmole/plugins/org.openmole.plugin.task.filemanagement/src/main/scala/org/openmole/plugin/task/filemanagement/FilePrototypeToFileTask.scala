/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.filemanagement

import java.io.File
import java.io.IOException
import java.util.Iterator
import java.util.LinkedList
import java.util.List

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError

import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import scala.collection.mutable.ListBuffer

import org.openmole.commons.tools.io.FileUtil.copy
import org.openmole.core.implementation.tools.VariableExpansion._

class FilePrototypeToFileTask(name: String, remove: Boolean = false) extends Task(name) {

  val toCopy = new ListBuffer[(IPrototype[File],String)]()
  val toCopyWithNameInVariable = new ListBuffer[(IPrototype[File], IPrototype[String], String)]()
  val listToCopyWithNameInVariable = new ListBuffer[(IPrototype[List[File]],IPrototype[List[String]],String)]()


  override def process(global: IContext, context: IContext, progress: IProgress)  {
    try{
      toCopy foreach( p => {
          val from = context.getValue(p._1)
          val to = new File(expandData(global, context, p._2))
          copy(from, to)

          if(remove) from.delete()

        })

      toCopyWithNameInVariable foreach( p => {
          val from = context.getValue(p._1)

          val name = context.getValue(p._2)
          val to = new File(expandData(global, context, p._3), name)
          copy(from, to)

          if(remove) from.delete()
        })

      listToCopyWithNameInVariable foreach ( cpList => {
          val files = context.getValue(cpList._1)
          val names = context.getValue(cpList._2)
          val urlDir = cpList._3;

          if(files != null && names != null) {

            val toDir = new File(expandData(global, context, urlDir))

            val itFile = files.iterator()
            val itName = names.iterator()

            while(itFile.hasNext() && itName.hasNext()) {
              val to = new File(toDir, itName.next())
              val from = itFile.next()
              copy(from, to)

              if(remove) from.delete()
            }

          }
        } )
    } catch {
      case e: IOException => throw new InternalProcessingError(e)
    }
  }
  
  def saveInputFile(prot:Any with IPrototype[File], url: String) {
    toCopy += ((prot, url))
    addInput(prot)
  }

  def saveInputFileAs(prot: IPrototype[File], name: IPrototype[String], dir: String) {
    toCopyWithNameInVariable += ((prot, name, dir))
    addInput(prot);
    addInput(name);
  }

  def saveInputFilesAs(fileProt: IPrototype[List[File]], nameProt: IPrototype[List[String]], dirUrl: String) {
    listToCopyWithNameInVariable += ((fileProt,nameProt, dirUrl))
    addInput(fileProt)
    addInput(nameProt)
  }


}
