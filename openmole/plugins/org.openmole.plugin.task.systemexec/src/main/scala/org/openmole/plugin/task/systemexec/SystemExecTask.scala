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

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.tools.io.{ StringBuilderOutputStream, FileUtil }
import org.openmole.core.tools.service.{ Logger, OS, ProcessUtil }
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.VariableExpansion
import ProcessUtil._
import java.io.File
import java.io.IOException
import java.io.PrintStream
import org.apache.commons.exec.CommandLine
import org.openmole.core.workflow.data._
import org.openmole.core.tools.service.Logger
import org.openmole.plugin.task.external._
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import FileUtil._

object SystemExecTask extends Logger {

  /**
   * System exec task execute an external process.
   * To communicate with the dataflow the result should be either a file / category or the return
   * value of the process.
   *
   * @param name the name of the task
   * @param commands the commands to run. The command is expanded before evaluation.
   * @param directory a subdirectory of the workspace that serve as reference for the command
   * execution and the files deployment / gathering
   * @param errorOnReturnCode if true an exception is thrown by the task in case the return
   * value of the process is not 0
   * @param returnValue optionally a prototype to output the return value of the process
   * @param output optionally a prototype to output the standard output of the process
   * @param error optionally a prototype to output the standard error output of the process
   *
   */
  def apply(commands: String*) =
    new SystemExecTaskBuilder(commands: _*)

}

abstract class SystemExecTask(
    val command: Seq[Commands],
    val directory: Option[String],
    val errorOnReturnCode: Boolean,
    val returnValue: Option[Prototype[Int]],
    val output: Option[Prototype[String]],
    val error: Option[Prototype[String]],
    val variables: Seq[(Prototype[_], String)]) extends ExternalTask {

  override protected def process(context: Context) = withWorkDir { tmpDir ⇒
    val workDir =
      directory match {
        case None    ⇒ tmpDir
        case Some(d) ⇒ new File(tmpDir, d)
      }

    def workDirPath = directory.getOrElse("")
    prepareInputFiles(context, tmpDir, workDirPath)

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

    def commandLine(cmd: String): String =
      CommandLine.parse(workDir.getAbsolutePath + File.separator + VariableExpansion(context + Variable(ExternalTask.PWD, workDir.getAbsolutePath), cmd)).toString

    def execute(command: String, out: PrintStream, err: PrintStream): Int = {
      try {
        val runtime = Runtime.getRuntime

        //FIXES java.io.IOException: error=26
        val process = runtime.synchronized {
          runtime.exec(
            command,
            variables.map { case (p, v) ⇒ v + "=" + context(p).toString }.toArray,
            workDir)
        }

        executeProcess(process, out, err)
      }
      catch {
        case e: IOException ⇒ throw new InternalProcessingError(e,
          s"""Error executing: ${command}
            |The content of the working directory was:
            |${workDir.listRecursive(_ ⇒ true).map(_.getPath).mkString("\n")}
          """.stripMargin
        )
      }
    }

    val osCommandLines: Seq[String] = command.find { _.os.compatible }.map { _.parts }.getOrElse(throw new UserBadDataError("Not command line found for " + OS.actualOS))

    def execAll(cmds: List[String]): Int =
      cmds match {
        case Nil ⇒ 0
        case cmd :: t ⇒
          val retCode = execute(commandLine(cmd), out, err)
          if (errorOnReturnCode && retCode != 0) throw new InternalProcessingError("Error executing: " + commandLine(cmd) + " return code was not 0 but " + retCode)
          if (t.isEmpty || (!errorOnReturnCode && retCode != 0)) retCode
          else execAll(t)
      }

    val retCode = execAll(osCommandLines.toList)
    val retContext: Context = fetchOutputFiles(context, workDir, workDirPath)

    retContext ++
      List(
        output.map { o ⇒ Variable(o, outBuilder.toString) },
        error.map { e ⇒ Variable(e, errBuilder.toString) },
        returnValue.map { r ⇒ Variable(r, retCode) }
      ).flatten

  }

}
