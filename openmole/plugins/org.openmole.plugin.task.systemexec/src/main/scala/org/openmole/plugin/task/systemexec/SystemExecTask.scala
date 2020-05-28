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

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion.FromContext
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external._
import org.openmole.tool.random._
import cats.syntax.traverse._
import org.openmole.core.dsl.outputs

object SystemExecTask {

  implicit def isTask: InputOutputBuilder[SystemExecTask] = InputOutputBuilder(SystemExecTask.config)
  implicit def isExternal: ExternalBuilder[SystemExecTask] = ExternalBuilder(SystemExecTask.external)
  implicit def isInfo = InfoBuilder(info)

  @deprecated
  implicit def isSystemExec = new SystemExecTaskBuilder[SystemExecTask] {
    override def commands = SystemExecTask.commands
    override def environmentVariables = SystemExecTask.environmentVariables
    override def returnValue = SystemExecTask.returnValue
    override def errorOnReturnValue = SystemExecTask.errorOnReturnValue
    override def stdOut = SystemExecTask.stdOut
    override def stdErr = SystemExecTask.stdErr
    override def workDirectory = SystemExecTask.workDirectory
  }

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(commands: Command*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): SystemExecTask =
    SystemExecTask(
      commands = commands.toVector
    )

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   */
  def apply(
    commands:             Seq[Command],
    workDirectory:        OptionalArgument[String]      = None,
    errorOnReturnValue:   Boolean                       = true,
    returnValue:          OptionalArgument[Val[Int]]    = None,
    stdOut:               OptionalArgument[Val[String]] = None,
    stdErr:               OptionalArgument[Val[String]] = None,
    shell:                Shell                         = Bash,
    environmentVariables: Vector[EnvironmentVariable]   = Vector.empty)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): SystemExecTask =
    new SystemExecTask(
      commands = commands.map(c ⇒ OSCommands(OS(), c)).toVector,
      workDirectory = workDirectory,
      errorOnReturnValue = errorOnReturnValue,
      returnValue = returnValue,
      stdOut = stdOut,
      stdErr = stdErr,
      shell = shell,
      environmentVariables = environmentVariables,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig()
    ) set (outputs += (Seq(returnValue.option, stdOut.option, stdErr.option).flatten: _*))

}

@Lenses case class SystemExecTask(
  commands:             Vector[OSCommands],
  workDirectory:        Option[String],
  errorOnReturnValue:   Boolean,
  returnValue:          Option[Val[Int]],
  stdOut:               Option[Val[String]],
  stdErr:               Option[Val[String]],
  shell:                Shell,
  environmentVariables: Vector[EnvironmentVariable],
  config:               InputOutputConfig,
  external:             External,
  info:                 InfoConfig
) extends Task with ValidateTask {

  override def validate =
    Validate { p ⇒
      import p._

      val allInputs = External.PWD :: inputs.toList

      val commandsError =
        for {
          c ← commands
          exp ← c.expanded
          e ← exp.validate(allInputs)
        } yield e

      val variableErrors = environmentVariables.flatMap(v ⇒ Seq(v.name, v.value)).flatMap(_.validate(allInputs))

      commandsError ++ variableErrors ++ External.validate(external)(allInputs).apply
    }

  override protected def process(executionContext: TaskExecutionContext) = FromContext { p ⇒
    import p._

    newFile.withTmpDir { tmpDir ⇒
      val workDir =
        workDirectory match {
          case None    ⇒ tmpDir
          case Some(d) ⇒ new File(tmpDir, d)
        }

      workDir.mkdirs()

      val context = p.context + (External.PWD → workDir.getAbsolutePath)

      val preparedContext = External.deployInputFilesAndResources(external, context, External.relativeResolver(workDir))

      val osCommandLines =
        commands.find { _.os.compatible }.map {
          cmd ⇒ cmd.expanded
        }.getOrElse(throw new UserBadDataError("No command line found for " + OS.actualOS))

      val expandedCommands = osCommandLines.map(_.from(preparedContext))

      val shellCommands =
        shell match {
          case Bash    ⇒ expandedCommands.map(cmd ⇒ ExecutionCommand.Parsed("bash", "-c", cmd))
          case NoShell ⇒ expandedCommands.map(ExecutionCommand.Raw(_))
        }

      val executionResult =
        executeAll(
          workDir,
          environmentVariables.map { v ⇒ v.name.from(context) → v.value.from(preparedContext) },
          shellCommands.toList,
          errorOnReturnValue && !returnValue.isDefined,
          stdOut.isDefined,
          stdErr.isDefined,
          stdOut = executionContext.outputRedirection.output,
          stdErr = executionContext.outputRedirection.output
        )

      val retContext: Context = External.fetchOutputFiles(external, outputs, preparedContext, External.relativeResolver(workDir), Seq(tmpDir))

      retContext ++
        List(
          stdOut.map { o ⇒ Variable(o, executionResult.output.get) },
          stdErr.map { e ⇒ Variable(e, executionResult.errorOutput.get) },
          returnValue.map { r ⇒ Variable(r, executionResult.returnCode) }
        ).flatten
    }
  }

}
