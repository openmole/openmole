/*
 * Copyright (C) 2011 Leclaire Mathieu  <mathieu.leclaire at openmole.org>
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

package org.openmole.plugin.task.filemanagement

import java.io.File
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.{IContext,IPrototype}
import org.openmole.core.model.execution.IProgress

class DeleteFileTask(name: String) extends Task(name) {
  
  var toDelete = List[IPrototype[File]]()
  var toDeleteList = List[IPrototype[Array[File]]]()
        
  override def process(context: IContext, progress: IProgress)  = {
    def toValue[T](p:IPrototype[T]) = context.value(p).getOrElse(throw new UserBadDataError("Unknown variable " + p.toString))
    
    toDelete.map(toValue(_)).foreach(_.delete)
    toDeleteList.flatMap(toValue(_)).foreach(_.delete)                
  }
   
  def deleteInputFile(prot : IPrototype[File]) = {
    toDelete = prot :: toDelete
    super.addInput(prot)
  }
  
  def deleteInputFileList (prot : IPrototype[Array[File]]) = {
    toDeleteList = prot :: toDeleteList
    super.addInput(prot)
  }
}