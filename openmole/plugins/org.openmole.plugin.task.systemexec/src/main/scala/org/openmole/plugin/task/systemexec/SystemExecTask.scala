/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.plugin.task.systemexec

import java.io.File
import java.io.IOException

import java.util.LinkedList

import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.launcher.CommandLauncherFactory
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.plugin.tools.utils.ProcessUtils._
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext

import java.util.logging.Level
import java.util.logging.Logger
import org.apache.commons.exec.CommandLine
import org.openmole.core.implementation.data.Prototype
import org.openmole.plugin.task.external.ExternalSystemTask
import org.openmole.plugin.task.systemexec.internal.Activator._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.tools.VariableExpansion._
import scala.collection.JavaConversions._


class SystemExecTask(name: String, val cmd: String, val returnValue: Prototype[Integer], relativeDir: String) extends ExternalSystemTask(name) {
  if(returnValue != null) addOutput(returnValue)
  
  def this(name: String, cmd: String) = {
    this(name, cmd, null, "")
  }
  
  def this(name: String, cmd: String, relativeDir: String) = {
    this(name, cmd, null, relativeDir)
  }
  
  def this(name: String, cmd: String, returnValue: Prototype[Integer]) = {
    this(name, cmd, returnValue, "")
  }
  
  def this(name: String, cmd: String, relativeDir: String, returnValue: Prototype[Integer]) = {
    this(name, cmd, returnValue, relativeDir)
  }
  
  override protected def process(global: IContext, context: IContext, progress: IProgress) = {
    try {
      val tmpDir = workspace.newDir("systemExecTask")

      prepareInputFiles(global, context, progress, tmpDir)
      val workDir = if(relativeDir.isEmpty) tmpDir else new File(tmpDir, relativeDir)
      val commandLine = CommandLine.parse(workDir.getAbsolutePath + File.separator + expandData(global, context, CommonVariables(workDir.getAbsolutePath), cmd))
      
      try {                    
       // val executor = new DefaultExecutor
       // executor.setWorkingDirectory(workDir)
       // val ret = executor.execute(commandLine);
     
        val process = Runtime.getRuntime().exec(commandLine.toString, null, workDir)
        val ret = executeProcess(process, System.out, System.err)
        if(returnValue != null) context.setValue[Integer](returnValue, ret)
      } catch {
        case e: IOException => throw new InternalProcessingError(e, "Error executing: " + commandLine)
      }

      fetchOutputFiles(global, context, progress, workDir)

    } catch {
      case e: IOException => throw new InternalProcessingError(e)
    }
  }

}
