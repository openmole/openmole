package org.openmole.gui.server.core

import org.openmole.core.event.EventAccumulator
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.gui.misc.utils.Utils._
import org.openmole.tool.file._
import org.openmole.core.workspace.{ AuthenticationProvider, Workspace }
import org.openmole.gui.shared._
import org.openmole.gui.ext.data.{ ScriptData, TreeNodeData }
import java.io.{ File, PrintStream }
import java.nio.file._
import org.openmole.console._
import scala.util.{ Failure, Success, Try }
import org.openmole.gui.ext.data._
import org.openmole.console.ConsoleVariables
import org.openmole.core.workflow.mole.{ MoleExecution, ExecutionContext }
import org.openmole.core.workflow.puzzle.Puzzle
import org.openmole.tool.stream.{ StringPrintStream, StringOutputStream }

/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object ApiImpl extends Api {

  val execution = new Execution

  //AUTHENTICATIONS
  def addAuthentication(data: AuthenticationData): Unit = ServerFactories.authenticationFactories(data.getClass).buildAuthentication(data)

  def authentications(): Seq[AuthenticationData] = ServerFactories.authenticationFactories.values.flatMap {
    _.allAuthenticationData
  }.toSeq

  def removeAuthentication(data: AuthenticationData) = {}

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean = Workspace.passwordIsCorrect(pass)

  def passwordChosen(): Boolean = Workspace.passwordChosen

  def resetPassword(): Unit = Workspace.reset

  def setPassword(pass: String): Boolean = try {
    Workspace.setPassword(pass)
    true
  }
  catch {
    case e: UserBadDataError ⇒ false
  }

  // FILES
  def addDirectory(treeNodeData: TreeNodeData, directoryName: String): Boolean = new File(treeNodeData.canonicalPath, directoryName).mkdirs

  def addFile(treeNodeData: TreeNodeData, fileName: String): Boolean = new File(treeNodeData.canonicalPath, fileName).createNewFile

  def deleteFile(treeNodeData: TreeNodeData): Unit = new File(treeNodeData.canonicalPath).recursiveDelete

  def fileSize(treeNodeData: TreeNodeData): Long = new File(treeNodeData.canonicalPath).length

  def listFiles(tnd: TreeNodeData): Seq[TreeNodeData] = Utils.listFiles(tnd.canonicalPath)

  def renameFile(treeNodeData: TreeNodeData, name: String): Boolean = {
    val canonicalFile = new File(treeNodeData.canonicalPath)
    val (source, target) = (canonicalFile, new File(canonicalFile.getParent, name))

    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    target.exists
  }

  def saveFile(path: String, fileContent: String): Unit = new File(path).content = fileContent

  def workspacePath(): String = Utils.workspaceProjectFile.getCanonicalPath()

  // EXECUTIONS

  def allExecutionStates(): Seq[(ExecutionId, ExecutionInfo)] = execution.allStates

  def allSaticInfos(): Seq[(ExecutionId, StaticExecutionInfo)] = execution.allStaticInfos

  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)

  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def runScript(scriptData: ScriptData): Unit = {
    val id = getUUID
    val projectsPath = Utils.workspaceProjectFile
    val console = new Console
    val repl = console.newREPL(ConsoleVariables(
      inputDirectory = new File(projectsPath, scriptData.inputDirectory),
      outputDirectory = new File(projectsPath, scriptData.outputDirectory)
    ))

    val execId = ExecutionId(id)

    def error(t: Throwable): Unit = execution.add(execId, Failed(ErrorBuilder(t)))
    def message(message: String): Unit = execution.add(execId, Failed(Error(message)))

    Try(repl.eval(scriptData.script)) match {
      case Failure(e) ⇒ error(e)
      case Success(o) ⇒
        o match {
          case puzzle: Puzzle ⇒
            val outputStream = new StringPrintStream()
            val accumulator = EventAccumulator(puzzle.environments) {
              case e @ (env, ex: ExceptionRaised) ⇒ e
            }
            Try(puzzle.toExecution(executionContext = ExecutionContext(out = outputStream))) match {
              case Success(ex) ⇒
                Try(ex.start) match {
                  case Failure(e)  ⇒ error(e)
                  case Success(ex) ⇒ execution.add(execId, StaticExecutionInfo(scriptData.scriptName, scriptData.script, ex.startTime.get), ex, outputStream)
                }
              case Failure(e) ⇒ error(e)
            }
          case _ ⇒ message("A puzzle have to be provided, the workflow can not be launched")
        }
    }
  }
}
