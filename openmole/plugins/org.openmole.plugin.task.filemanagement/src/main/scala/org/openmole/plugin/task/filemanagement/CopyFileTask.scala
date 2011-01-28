/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

import java.util.logging.Logger
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError

import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.data.IContext
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IPrototype
import scala.collection.mutable.ListBuffer

import org.openmole.commons.tools.io.FileUtil._
import org.openmole.core.implementation.tools.VariableExpansion._

class CopyFileTask(name: String, remove: Boolean = false) extends Task(name) {

  def this(name: String) = this(name, false)
  
  val toCopy = new ListBuffer[(IPrototype[File],String)]()
  val toCopyWithNameInVariable = new ListBuffer[(IPrototype[File], IPrototype[String], String)]()
  val listToCopyWithNameInVariable = new ListBuffer[(IPrototype[Array[File]],IPrototype[Array[String]],String)]()

  override def process(context: IContext, progress: IProgress)  {
    try{
      toCopy foreach( p => {
          val from = context.value(p._1).get
          val to = new File(expandData(context, p._2))
          
          Logger.getLogger(classOf[CopyFileTask].getName).fine("From " + from.getAbsolutePath + " to " + to.getAbsolutePath)
          
          to.getParentFile.mkdirs
          copy(from, to)

          if(remove) from.recursiveDelete
        })

      toCopyWithNameInVariable foreach( p => {
          val from = context.value(p._1).get
          val name = context.value(p._2).get
          
          val dir = new File(expandData(context, p._3))
          dir.mkdirs
       
          val to = new File(dir, name)
          
          Logger.getLogger(classOf[CopyFileTask].getName).fine("From " + from.getAbsolutePath + " to " + to.getAbsolutePath)
             
          copy(from, to)

          if(remove) from.recursiveDelete
        })

      listToCopyWithNameInVariable foreach ( cpList => {
          val files = context.value(cpList._1).get
          val names = context.value(cpList._2).get
          val urlDir = cpList._3

          if(files != null && names != null) {

            val toDir = new File(expandData(context, urlDir))
            toDir.mkdirs
            
            val itFile = files.iterator
            val itName = names.iterator

            while(itFile.hasNext && itName.hasNext) {
              val to = new File(toDir, itName.next)
              val from = itFile.next
              
              Logger.getLogger(classOf[CopyFileTask].getName).fine("From " + from.getAbsolutePath + " to " + to.getAbsolutePath)
              
              copy(from, to)              
              if(remove) from.recursiveDelete
            }

          }
        } )
    } catch {
      case e: IOException => throw new InternalProcessingError(e)
    }
  }
  
  def save(prot:Any with IPrototype[File], url: String) {
    toCopy += prot -> url
    addInput(prot)
  }

  def save(prot: IPrototype[File], name: IPrototype[String], dir: String) {
    toCopyWithNameInVariable += ((prot, name, dir))
    addInput(prot)
    addInput(name)
  }

  def saveList(fileProt: IPrototype[Array[File]], nameProt: IPrototype[Array[String]], dirUrl: String) {
    listToCopyWithNameInVariable += ((fileProt,nameProt, dirUrl))
    addInput(fileProt)
    addInput(nameProt)
  }


}
