/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
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

package org.openmole.plugin.task.systemexec

import java.io.File
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.data.IContext
import org.openmole.plugin.task.external.system.ExternalSystemTask
import java.io.IOException
import org.openmole.plugin.task.systemexec.internal.Activator._
import org.openmole.core.implementation.tools.VariableExpansion._
import org.apache.commons.exec.CommandLine
import org.openmole.plugin.tools.utils.ProcessUtils._
import scala.collection.JavaConversions._
import java.lang.Integer

abstract class AbstractSystemExecTask (name: String, 
                                       val cmd: String, 
                                       val returnValue: IPrototype[Integer] = null, 
                                       relativeDir: String = "") extends ExternalSystemTask(name) {
  if(returnValue != null) addOutput(returnValue)
  
//  def this(name: String, cmd: String) = {
//    this(name, cmd, null, "")
//  }
//  
//  def this(name: String, cmd: String, relativeDir: String) = {
//    this(name, cmd, null, relativeDir)
//  }
//  
//  def this(name: String, cmd: String, returnValue: Prototype[Integer]) = {
//    this(name, cmd, returnValue, "")
//  }
//  
//  def this(name: String, cmd: String, relativeDir: String, returnValue: Prototype[Integer]) = {
//    this(name, cmd, returnValue, relativeDir)
//  }
//  
  
  override protected def process(global: IContext, context: IContext, progress: IProgress) = {
      val tmpDir = workspace.newDir("systemExecTask")

      prepareInputFiles(global, context, progress, tmpDir)
      val workDir = if(relativeDir.isEmpty) tmpDir else new File(tmpDir, relativeDir)
      val commandLine = CommandLine.parse(workDir.getAbsolutePath + File.separator + expandData(global, context, CommonVariables(workDir.getAbsolutePath), cmd))
      
      try {                    
        // val executor = new DefaultExecutor
        // executor.setWorkingDirectory(workDir)
        // val ret = executor.execute(commandLine);
     
        val process = Runtime.getRuntime.exec(commandLine.toString, null, workDir)
        val ret = execute(process,context)
        if(returnValue != null) context += (returnValue, ret)
        // if(returnValue != null) context.setValue[Integer](returnValue, ret)
      } catch {
        case e: IOException => throw new InternalProcessingError(e, "Error executing: " + commandLine)
      }

      fetchOutputFiles(global, context, progress, workDir)
  }
  
  protected def execute(process: Process, context: IContext):Integer
}
