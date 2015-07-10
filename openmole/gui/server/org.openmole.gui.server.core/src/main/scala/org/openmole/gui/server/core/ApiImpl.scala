package org.openmole.gui.server.core

import org.openmole.core.event._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.gui.misc.utils.Utils._
import org.openmole.gui.server.core.Utils._
import org.openmole.tool.file._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.shared._
import org.openmole.gui.ext.data.{ ScriptData, TreeNodeData }
import java.io.File
import java.nio.file._
import org.openmole.console._
import scala.util.{ Failure, Success, Try }
import org.openmole.gui.ext.data._
import org.openmole.console.ConsoleVariables
import org.openmole.core.workflow.mole.ExecutionContext
import org.openmole.core.workflow.puzzle.Puzzle
import org.openmole.tool.stream.StringPrintStream
import scala.concurrent.stm._

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

  implicit def authProvider = Workspace.authenticationProvider

  //AUTHENTICATIONS
  def addAuthentication(data: AuthenticationData): Unit = ServerFactories.authenticationFactories(data.getClass).buildAuthentication(data)

  def authentications(): Seq[AuthenticationData] = ServerFactories.authenticationFactories.values.flatMap {
    _.allAuthenticationData
  }.toSeq

  def removeAuthentication(data: AuthenticationData) = ServerFactories.authenticationFactories(data.getClass).removeAuthentication(data)

  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean = Workspace.passwordIsCorrect(pass)

  def passwordState() = PasswordState(chosen = Workspace.passwordChosen, hasBeenSet = Workspace.passwordHasBeenSet)

  def resetPassword(): Unit = Workspace.reset

  def setPassword(pass: String): Boolean = try {
    Workspace.setPassword(pass)
    true
  }
  catch {
    case e: UserBadDataError ⇒ false
  }

  // FILES
  def addDirectory(treeNodeData: TreeNodeData, directoryName: String): Boolean =
    new File(new File(treeNodeData.canonicalPath.path), directoryName).mkdirs

  def addFile(treeNodeData: TreeNodeData, fileName: String): Boolean =
    new File(new File(treeNodeData.canonicalPath.path), fileName).createNewFile

  def deleteFile(treeNodeData: TreeNodeData): Unit = new File(treeNodeData.canonicalPath.path).recursiveDelete

  def diff(subPath: SafePath, fullPath: SafePath): SafePath =
    SafePath.sp(new File(fullPath.path).getCanonicalPath diff new File(subPath.path).getParentFile.getCanonicalPath, subPath.leaf)

  def fileSize(treeNodeData: TreeNodeData): Long = new File(treeNodeData.canonicalPath.path).length

  def listFiles(tnd: TreeNodeData): Seq[TreeNodeData] = Utils.listFiles(tnd.canonicalPath)

  def renameFile(treeNodeData: TreeNodeData, name: String): TreeNodeData =
    renameFileFromPath(treeNodeData.canonicalPath, name)

  def renameFileFromPath(filePath: SafePath, newName: String): TreeNodeData = {
    val targetFile = new File(new File(filePath.path).getParentFile, newName)
    val (source, target) = (new File(filePath.path), targetFile)

    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
    TreeNodeData(newName, targetFile, false, 0L, "")

  }

  def saveFile(path: String, fileContent: String): Unit = new File(path).content = fileContent

  def workspaceProjectNode(): TreeNodeData = Utils.workspaceProjectFile

  def authenticationKeysPath(): SafePath = Utils.authenticationKeysFile

  // EXECUTIONS

  def allExecutionStates(): Seq[(ExecutionId, ExecutionInfo)] = execution.allStates

  def allStaticInfos(): Seq[(ExecutionId, StaticExecutionInfo)] = execution.allStaticInfos

  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)

  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def runScript(scriptData: ScriptData): Unit = {
    val id = getUUID
    val projectsPath = Utils.workspaceProjectFile
    val console = new Console
    // FIXME set workdirectory
    val repl = console.newREPL(ConsoleVariables()())

    val execId = ExecutionId(id)

    def error(t: Throwable): Unit = execution.add(execId, Failed(ErrorBuilder(t)))
    def message(message: String): Unit = execution.add(execId, Failed(Error(message)))

    Try(repl.eval(scriptData.script)) match {
      case Failure(e) ⇒ error(e)
      case Success(o) ⇒
        o match {
          case puzzle: Puzzle ⇒
            val outputStream = new StringPrintStream()

            puzzle.environments.values.foreach { env ⇒
              val envId = EnvironmentId(getUUID)
              Runnings.add(execId, puzzle.environments.values.map { env ⇒ (envId, env) }.toSeq, outputStream)
              env.listen {
                case (env, ex: ExceptionRaised) ⇒ Runnings.append(execId, envId, env, ex)
              }
            }
            Try(puzzle.toExecution(executionContext = ExecutionContext(out = outputStream))) match {
              case Success(ex) ⇒
                Try(ex.start) match {
                  case Failure(e)  ⇒ error(e)
                  case Success(ex) ⇒ execution.add(execId, StaticExecutionInfo(scriptData.scriptName, scriptData.script, ex.startTime.get), DynamicExecutionInfo(ex, outputStream))
                }
              case Failure(e) ⇒ error(e)
            }
          case _ ⇒ message("A puzzle have to be provided, the workflow can not be launched")
        }
    }
  }

  def runningErrorEnvironmentAndOutputData(): (Seq[RunningEnvironmentData], Seq[RunningOutputData]) = atomic { implicit ctx ⇒
    val envIds = Runnings.ids
    (
      envIds.map {
        case (id, envIds) ⇒
          RunningEnvironmentData(id, Runnings.runningEnvironments(id).flatMap { _._2.environmentError })
      }.toSeq,
      envIds.keys.toSeq.map {
        Runnings.outputsDatas(_)
      }
    )
  }

}
