/*
 *  Copyright (C) 2010 Romain Reuillon <romain.Romain Reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.tools._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.exception._
import org.openmole.misc.tools.io.StringBuilderOutputStream
import org.openmole.misc.tools.service.ProcessUtil._
import java.io.File
import java.io.IOException
import java.io.PrintStream
import org.apache.commons.exec.CommandLine
import org.openmole.core.implementation.data._
import org.openmole.misc.workspace._
import org.openmole.misc.tools.service.OS
import org.openmole.plugin.task.external._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object SystemExecTask {

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / dir or the return
   * value of the process.
   *
   * @param name the name of the task
   * @param command the command to run. The command is expanded before evalualtion.
   * @param directory a subdirectory of the workspace that serve as reference for the command
   * execution and the files deployment / gathering
   * @param errorOnReturnCode if true an exception is thrown by the task in case the return
   * value of the process is not 0
   * @param returnValue optionally a prototype to output the return value of the process
   * @param output optionally a prototype to output the standard output of the process
   * @param error optionally a prototype to output the standard error output of the process
   *
   */
  def apply(
    name: String,
    _command: Option[String] = None,
    directory: String = "",
    errorOnReturnCode: Boolean = true,
    returnValue: Option[Prototype[Int]] = None,
    output: Option[Prototype[String]] = None,
    error: Option[Prototype[String]] = None)(implicit plugins: PluginSet) = new ExternalTaskBuilder { builder ⇒

    List(output, error, returnValue).flatten.foreach(p ⇒ addOutput(p))

    private val _variables = new ListBuffer[(Prototype[_], String)]
    private val _commands = new ListBuffer[(String, OS)]

    _command.foreach(command(_))

    def variables = _variables.toList

    /**
     * Add variable from openmole to the environment of the system exec task. The
     * environment variable is set using a toString of the openmole variable content.
     *
     * @param prototype the prototype of the openmole variable to inject in the environment
     * @param variable the name of the environment variable. By default the name of the environment
     * variable is the same as the one of the openmole protoype.
     */
    def addVariable(prototype: Prototype[_], variable: Option[String] = None): this.type = {
      _variables += prototype -> variable.getOrElse(prototype.name)
      addInput(prototype)
      this
    }

    def command(cmd: String, os: OS = OS()) = _commands += (cmd -> os)

    def toTask = new SystemExecTask(name, _commands, directory, errorOnReturnCode, returnValue, output, error, variables) with builder.Built
  }

}

sealed abstract class SystemExecTask(
    val name: String,
    val command: Iterable[(String, OS)],
    val directory: String,
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Iterable[(Prototype[_], String)]) extends ExternalTask {

  override protected def process(context: Context) = {
    val tmpDir = Workspace.newDir("systemExecTask")

    val workDir = if (directory.isEmpty) tmpDir else new File(tmpDir, directory)
    val links = prepareInputFiles(context, tmpDir, directory)

    val osCommandLine: String = command.find { case (_, os) ⇒ os.compatible }.map { case (cmd, _) ⇒ cmd }.getOrElse(throw new UserBadDataError("Not command line found for " + OS.actualOS))

    val commandLine =
      CommandLine.parse(workDir.getAbsolutePath + File.separator + VariableExpansion(context + Variable(ExternalTask.PWD, workDir.getAbsolutePath), osCommandLine))

    try {
      val f = new File(commandLine.getExecutable)
      val process = Runtime.getRuntime.exec(
        commandLine.toString,
        variables.map { case (p, v) ⇒ v + "=" + context(p).toString }.toArray,
        workDir)

      execute(process, context) match {
        case (retCode, variables) ⇒
          if (errorOnReturnCode && retCode != 0) throw new InternalProcessingError("Error executing: " + commandLine + " return code was not 0 but " + retCode)

          val retContext = fetchOutputFiles(context, workDir, links) ++ variables

          returnValue match {
            case None              ⇒ retContext
            case Some(returnValue) ⇒ retContext + (returnValue, retCode)
          }
      }
    }
    catch {
      case e: IOException ⇒ throw new InternalProcessingError(e, "Error executing: " + commandLine)
    }
  }

  protected def execute(process: Process, context: Context) = {
    val outBuilder = new StringBuilder
    val errBuilder = new StringBuilder

    val out = output match {
      case Some(_) ⇒ new PrintStream(new StringBuilderOutputStream(outBuilder))
      case None    ⇒ System.out
    }
    val err = error match {
      case Some(_) ⇒ new PrintStream(new StringBuilderOutputStream(errBuilder))
      case None    ⇒ System.err
    }

    val r = executeProcess(process, out, err)

    (
      r,
      List(
        output.map { o ⇒ Variable(o, outBuilder.toString) },
        error.map { e ⇒ Variable(e, errBuilder.toString) }).flatten)
  }
}
