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

import org.openmole.core.exception.{ InternalProcessingError, MultipleException, UserBadDataError }
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.workflow.tools.VariableExpansion.Expansion
import org.openmole.tool.file._
import org.openmole.core.tools.service.{ OS, ProcessUtil }
import ProcessUtil._
import java.io.File

import monocle.macros.Lenses
import org.openmole.core.workflow.data._
import org.openmole.plugin.task.external._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external.ExternalTask._
import org.openmole.core.workflow.dsl._

import scala.annotation.tailrec

object SystemExecTask {

  case class ExpandedSystemExecCommand(expandedCommand: Expansion)

  implicit def isBuilder[T] = new SystemExecTaskBuilder[SystemExecTask] {
    override def commands = SystemExecTask.command
    override def environmentVariables = SystemExecTask.environmentVariables
    override def resources = SystemExecTask.resources
    override def outputFiles = SystemExecTask.outputFiles
    override def inputFileArrays = SystemExecTask.inputFileArrays
    override def inputFiles = SystemExecTask.inputFiles
    override def defaults = SystemExecTask.defaults
    override def workDirectory = SystemExecTask.workDirectory
    override def outputs = SystemExecTask._outputs
    override def name = SystemExecTask.name
    override def inputs = SystemExecTask.inputs
    override def returnValue = SystemExecTask.returnValue
    override def errorOnReturnValue = SystemExecTask.errorOnReturnValue
    override def stdOut = SystemExecTask.stdOut
    override def stdErr = SystemExecTask.stdErr
  }

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(commands: Command*): SystemExecTask =
    new SystemExecTask(
      command = Vector.empty,
      workDirectory = None,
      errorOnReturnValue = true,
      returnValue = None,
      stdOut = None,
      stdErr = None,
      environmentVariables = Vector.empty,
      inputs = PrototypeSet.empty,
      _outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None,
      inputFiles = Vector.empty,
      inputFileArrays = Vector.empty,
      outputFiles = Vector.empty,
      resources = Vector.empty
    ) set (pack.commands += (OS(), commands: _*))
}

@Lenses case class SystemExecTask(
    command:              Vector[OSCommands],
    workDirectory:        Option[String],
    errorOnReturnValue:   Boolean,
    returnValue:          Option[Prototype[Int]],
    stdOut:               Option[Prototype[String]],
    stdErr:               Option[Prototype[String]],
    environmentVariables: Vector[(Prototype[_], String)],
    inputs:               PrototypeSet,
    _outputs:             PrototypeSet,
    defaults:             DefaultSet,
    name:                 Option[String],
    inputFiles:           Vector[InputFile],
    inputFileArrays:      Vector[InputFileArray],
    outputFiles:          Vector[OutputFile],
    resources:            Vector[Resource]
) extends ExternalTask with ValidateTask {

  import SystemExecTask._

  def outputs = _outputs ++ Seq(stdOut, stdErr, returnValue).flatten

  override def validate =
    for {
      c ← command
      exp ← c.expanded
      e ← exp.validate(inputs.toSeq)
    } yield e

  @tailrec
  protected[systemexec] final def execAll(cmds: List[ExpandedSystemExecCommand], workDir: File, preparedContext: Context, acc: ExecutionResult = ExecutionResult.empty)(implicit rng: RandomProvider): ExecutionResult =
    cmds match {
      case Nil ⇒ acc
      case cmd :: t ⇒
        val commandline = commandLine(cmd.expandedCommand, workDir.getAbsolutePath, preparedContext)

        val result = execute(commandline, workDir, environmentVariables, preparedContext, returnOutput = stdOut.isDefined, returnError = stdErr.isDefined)
        if (errorOnReturnValue && !returnValue.isDefined && result.returnCode != 0)
          throw new InternalProcessingError(
            s"""Error executing command"}:
                 |[${commandline.mkString(" ")}] return code was not 0 but ${result.returnCode}""".stripMargin
          )
        else execAll(t, workDir, preparedContext, ExecutionResult.append(acc, result))
    }

  override protected def process(context: Context, executionContext: TaskExecutionContext)(implicit rng: RandomProvider) = withWorkDir(executionContext) { tmpDir ⇒
    val workDir =
      workDirectory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    workDir.mkdirs()

    val preparedContext = prepareInputFiles(context, relativeResolver(workDir))

    val osCommandLines: Seq[ExpandedSystemExecCommand] = command.find { _.os.compatible }.map {
      cmd ⇒ cmd.expanded map { expansion ⇒ ExpandedSystemExecCommand(expansion) }
    }.getOrElse(throw new UserBadDataError("No command line found for " + OS.actualOS))

    val executionResult = execAll(osCommandLines.toList, workDir, preparedContext)

    val retContext: Context = fetchOutputFiles(preparedContext, relativeResolver(workDir))
    checkAndClean(retContext, tmpDir)

    retContext ++
      List(
        stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
        stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
        returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
      ).flatten
  }

}
