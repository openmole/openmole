/*
 * Copyright (C) 2014 Romain Reuillon
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

import org.openmole.core.tools.service.OS
import org.openmole.plugin.task.external._
import org.openmole.core.workflow.data._

import scala.collection.mutable.ListBuffer

class SystemExecTaskBuilder(commands: Command*) extends ExternalTaskBuilder
    with ReturnValue
    with ErrorOnReturnCode
    with StdOutErr
    with EnvironmentVariables
    with WorkDirectory { builder ⇒

  protected val _commands = new ListBuffer[OSCommands]

  addCommand(OS(), commands: _*)

  def addCommand(os: OS, cmd: Command*): this.type = {
    _commands += OSCommands(os, cmd: _*)
    this
  }

  def toTask: SystemExecTask =
    new SystemExecTask(
      builder._commands.toList,
      builder.workDirectory,
      builder.errorOnReturnValue,
      builder.returnValue,
      builder.stdOut,
      builder.stdErr,
      builder.environmentVariables.toList) with builder.Built {
      override val outputs: PrototypeSet = builder.outputs + List(stdOut, stdErr, returnValue).flatten
    }

}
