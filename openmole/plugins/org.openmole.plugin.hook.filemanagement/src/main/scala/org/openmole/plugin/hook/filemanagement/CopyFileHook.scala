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

package org.openmole.plugin.hook.filemanagement

import java.io.File

import org.openmole.core.model.capsule.IGenericCapsule
import org.openmole.core.implementation.hook.CapsuleExecutionHook
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.ListBuffer

import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.misc.exception.UserBadDataError

class CopyFileHook(moleExecution: IMoleExecution, capsule: IGenericCapsule) extends CapsuleExecutionHook(moleExecution, capsule) {
  
  val toCopy = new ListBuffer[(IPrototype[File], String, Boolean)]()
  val toCopyWithNameInVariable = new ListBuffer[(IPrototype[File], IPrototype[String], String, Boolean)]()
  val listToCopyWithNameInVariable = new ListBuffer[(IPrototype[Array[File]],IPrototype[Array[String]], String, Boolean)]()

  override def process(moleJob: IMoleJob) = {
    import moleJob.context
    
    toCopy.foreach {
      case(prototype, name, delete) => {
          context.value(prototype) match {
            case Some(from) =>   
              val to = new File(expandData(context, name))
          
              to.getParentFile.mkdirs
              from.copy(to)

              if(delete) from.recursiveDelete
            case None => throw new UserBadDataError("No variable " + prototype + " found.")
          } 
        }
    }

    toCopyWithNameInVariable foreach {
      case(prototype, namePrototype, dirName, delete) => {
          (context.value(prototype), context.value(namePrototype)) match {
            case(Some(from), Some(name)) =>
              val dir = new File(expandData(context, dirName))
              dir.mkdirs
       
              val to = new File(dir, name)             
              from.copy(to)

              if(delete) from.recursiveDelete
            case(None, None) => throw new UserBadDataError("No variable " + prototype + " and " + namePrototype + " found.")
            case(Some(_), None) => throw new UserBadDataError("No variable " + prototype + " found.")
            case(None, Some(_)) => throw new UserBadDataError("No variable " + namePrototype + " found.")
          }
        }
    }

    listToCopyWithNameInVariable foreach {
      case(prototype, namePrototype, dirName, delete) => {
          (context.value(prototype), context.value(namePrototype)) match {
            case(Some(files), Some(names)) =>
              val toDir = new File(expandData(context, dirName))
              toDir.mkdirs

              files zip names foreach {
                case(from, name) =>
                  val to = new File(toDir, name)
              
                  from.copy(to)              
                  if(delete) from.recursiveDelete
              }
            case(None, None) => throw new UserBadDataError("No variable " + prototype + " and " + namePrototype + " found.")
            case(Some(_), None) => throw new UserBadDataError("No variable " + prototype + " found.")
            case(None, Some(_)) => throw new UserBadDataError("No variable " + namePrototype + " found.")
          }
        }
    }
  }
  
  def save(prot:Any with IPrototype[File], url: String): this.type = save(prot, url, false)
  def save(prot:Any with IPrototype[File], url: String, delete: Boolean): this.type = {toCopy += ((prot, url, delete)); this}
  
  def save(prot: IPrototype[File], name: IPrototype[String], dir: String): this.type = save(prot, name, dir, false)
  def save(prot: IPrototype[File], name: IPrototype[String], dir: String, delete: Boolean): this.type = {toCopyWithNameInVariable += ((prot, name, dir, delete)); this}

  def saveList(fileProt: IPrototype[Array[File]], nameProt: IPrototype[Array[String]], dirUrl: String): this.type = {listToCopyWithNameInVariable += ((fileProt,nameProt, dirUrl, false)); this}
  def saveList(fileProt: IPrototype[Array[File]], nameProt: IPrototype[Array[String]], dirUrl: String, delete: Boolean): this.type = {listToCopyWithNameInVariable += ((fileProt,nameProt, dirUrl, delete)); this}

}
