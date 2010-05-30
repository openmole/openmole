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

package org.openmole.plugin.task.systemexec

import java.io.File
import java.io.IOException

import java.util.LinkedList

import org.openmole.commons.exception.InternalProcessingError
import org.openmole.commons.exception.UserBadDataError
import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext

import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.ShutdownHookProcessDestroyer
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.mole.IExecutionContext
import org.openmole.plugin.task.external.ExternalSystemTask
import org.openmole.plugin.task.systemexec.internal.Activator._
import org.openmole.core.implementation.tools.VariableExpansion._
import scala.collection.JavaConversions._

class SystemExecTask(name: String, val cmd: String, val returnValue: Prototype[Integer] = null) extends ExternalSystemTask(name) {
  if(returnValue != null) addOutput(returnValue)

  object Prototypes {
    val PWD = new Prototype[File]("PWD", classOf[File])
  }
  
  def this(name: String, cmd: String) = {
    this(name, cmd, null)
  }

  override protected def process(context: IContext, executionContext: IExecutionContext, progress: IProgress) = {
    try {
      val tmpDir = workspace.newTmpDir("systemExecTask")

      prepareInputFiles(context, progress, tmpDir)

      val vals = new LinkedList[IVariable[_]]
      vals.add(new Variable(Prototypes.PWD, tmpDir))
      val commandLine = CommandLine.parse(expandData(context,vals, cmd))

      val executor = new DefaultExecutor
      executor.setProcessDestroyer(new ShutdownHookProcessDestroyer)
      executor.setWorkingDirectory(tmpDir)


      try {
        val ret: Integer = executor.execute(commandLine)
        if(returnValue != null) context.setValue(returnValue, ret)
      } catch {
        case e: IOException => throw new InternalProcessingError(e)
      }

      fetchOutputFiles(context, progress, tmpDir)

    } catch {
      case e: IOException => throw new InternalProcessingError(e)
    }
  }
}
