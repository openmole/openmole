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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import java.io.File
import org.openmole.core.model.data.IVariable
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IContext
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.task.external.ExternalTask
import org.openmole.plugin.task.external.system.ExternalSystemTask
import java.io.IOException
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.tools.VariableExpansion._
import org.apache.commons.exec.CommandLine
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.service.ProcessUtil._
import scala.collection.JavaConversions._

object AbstractSystemExecTask extends Logger

abstract class AbstractSystemExecTask (name: String, 
                                       val cmd: String, 
                                       val returnValue: Option[IPrototype[Int]] = null, 
                                       exceptionIfReturnValueNotZero: Boolean = true,
                                       relativeDir: String = "") extends ExternalSystemTask(name) {
 
  import AbstractSystemExecTask._
  
  returnValue match {
    case None =>
    case Some(returnValue) => addOutput(returnValue)
  }
  
  override protected def process(context: IContext) = {
    val tmpDir = Workspace.newDir("systemExecTask")

    val links = prepareInputFiles(context, tmpDir)
    val workDir = if(relativeDir.isEmpty) tmpDir else new File(tmpDir, relativeDir)
    val commandLine = CommandLine.parse( workDir.getAbsolutePath + File.separator + expandData(context, List(new Variable(ExternalTask.PWD, workDir.getAbsolutePath)), cmd))
      
    try {    
      val f = new File(commandLine.getExecutable)
      //logger.fine(f + " " + f.exists)
      val process = Runtime.getRuntime.exec(commandLine.toString, null, workDir)
      
      execute(process,context) match {
        case(retCode, variables) =>
          if(exceptionIfReturnValueNotZero && retCode != 0) throw new InternalProcessingError("Error executing: " + commandLine +" return code was not 0 but " + retCode)
        
          val retContext = fetchOutputFiles(context, workDir, links) ++ variables
      
          returnValue match {
            case None => retContext
            case Some(returnValue) => retContext + (returnValue, retCode)
          }
      }
    } catch {
      case e: IOException => throw new InternalProcessingError(e, "Error executing: " + commandLine)
    }
  }
  
  protected def execute(process: Process, context: IContext): (Int, Iterable[IVariable[_]])
}
