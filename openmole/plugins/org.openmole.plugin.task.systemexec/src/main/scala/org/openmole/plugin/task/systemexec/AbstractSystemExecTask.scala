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
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IContext
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.task.external.ExternalTask
import java.io.IOException
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.tools.VariableExpansion._
import org.apache.commons.exec.CommandLine
import org.openmole.misc.tools.service.ProcessUtil._
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object AbstractSystemExecTask {

  abstract class Builder extends ExternalTaskBuilder {
    private val _variables = new ListBuffer[(IPrototype[_], String)]

    def variables = _variables.toList

    def addVariable(prototype: IPrototype[_], variable: String): this.type = {
      _variables += prototype -> variable
      addInput(prototype)
      this
    }
    def addVariable(prototype: IPrototype[_]): this.type = addVariable(prototype, prototype.name)

  }

}

abstract class AbstractSystemExecTask extends ExternalTask {

  def cmd: String
  def returnValue: Option[IPrototype[Int]]
  def exceptionIfReturnValueNotZero: Boolean
  def dir: String
  def variables: Iterable[(String, String)]

  import AbstractSystemExecTask._

  override protected def process(context: IContext) = {
    val tmpDir = Workspace.newDir("systemExecTask")

    val workDir = if (dir.isEmpty) tmpDir else new File(tmpDir, dir)
    val links = prepareInputFiles(context, tmpDir, dir)
    val commandLine = CommandLine.parse(workDir.getAbsolutePath + File.separator + expandData(context, List(new Variable(ExternalTask.PWD, workDir.getAbsolutePath)), cmd))

    try {
      val f = new File(commandLine.getExecutable)
      //logger.fine(f + " " + f.exists)
      val process = Runtime.getRuntime.exec(
        commandLine.toString,
        variables.map { case (p, v) ⇒ v + "=" + context.valueOrException(p).toString }.toArray,
        workDir)

      execute(process, context) match {
        case (retCode, variables) ⇒
          if (exceptionIfReturnValueNotZero && retCode != 0) throw new InternalProcessingError("Error executing: " + commandLine + " return code was not 0 but " + retCode)

          val retContext = fetchOutputFiles(context, workDir, links) ++ variables

          returnValue match {
            case None ⇒ retContext
            case Some(returnValue) ⇒ retContext + (returnValue, retCode)
          }
      }
    } catch {
      case e: IOException ⇒ throw new InternalProcessingError(e, "Error executing: " + commandLine)
    }
  }

  protected def execute(process: Process, context: IContext): (Int, Iterable[IVariable[_]])
}
