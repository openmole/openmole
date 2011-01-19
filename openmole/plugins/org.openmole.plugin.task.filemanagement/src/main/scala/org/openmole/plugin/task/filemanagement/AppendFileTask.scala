/*
 *  Copyright (C) 2010 reuillon
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

import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.data.IContext
import java.io.File
import org.openmole.commons.tools.io.FileUtil.copy
import java.io.FileInputStream
import java.io.FileOutputStream
import org.openmole.commons.exception.UserBadDataError

/**
 * Appends a variable content to an existing file.
 * The content of toBeDumpedPrototype file is appended to the outputFile safely(
 * concurent accesses are treated).
 * In the case of directories, all the files of the original directory are append to the
 * files of the target one.
 */
class AppendFileTask(name: String, toBeDumpedPrototype: IPrototype[File], outputFile: String) extends Task(name) {
 
  override def process(global: IContext, context: IContext, progress: IProgress) = {
    val from = context value(toBeDumpedPrototype) get
    val to = new File(VariableExpansion.expandData(global,context,outputFile))
    if (!from.exists){
      throw new UserBadDataError("The file "+from+" does not exist.")
    }
    
    if (!to.exists){
      throw new UserBadDataError("The file "+to+" does not exist.")
    }
    
    if (from.isDirectory() && to.isDirectory()){
      val toFiles = to.list
      from.list foreach ( f => {
          if (!toFiles.contains(f)){
            new File(f).createNewFile()
          }
          lockAndAppendFile(new File(from,f),new File(to,f))
        })
    }
    else if (from.isFile && to.isFile){
      lockAndAppendFile(from,to)    
    }
    else {
      throw new UserBadDataError("The merge can only be done from a file to another or from a directory to another. ("+from.toString+" and "+to.toString+" found)")
    }
  }
  
  def lockAndAppendFile(from: String, to: String): Unit = lockAndAppendFile(new File(from), new File(to))
  
  
  def lockAndAppendFile(from: File,to: File): Unit = {
    val channelI = new FileInputStream(from) getChannel()
    try{
      val channelO = new FileOutputStream(to,true) getChannel()
      try{
        val lock = channelO lock()
        try{
          copy(channelI,channelO)
        }finally{
          lock release()
        }
      }finally {
        channelO close()   
      }
    }finally{
      channelI close()     
    }
    
  }
}

