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
import org.openmole.core.workflow.puzzle.{ PuzzleBuilder, Puzzle }
import org.openmole.tool.stream.StringPrintStream
import scala.concurrent.stm._
import org.openmole.tool.file._
import org.openmole.tool.tar._

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
  def addDirectory(treeNodeData: TreeNodeData, directoryName: String): Boolean = new File(treeNodeData.safePath, directoryName).mkdirs

  def addFile(treeNodeData: TreeNodeData, fileName: String): Boolean = new File(treeNodeData.safePath, fileName).createNewFile

  def deleteFile(treeNodeData: TreeNodeData): Unit = safePathToFile(treeNodeData.safePath).recursiveDelete

  def extractTGZ(treeNodeData: TreeNodeData): Unit = treeNodeData.safePath.extension match {
    case FileExtension.TGZ ⇒
      val archiveFile = safePathToFile(treeNodeData.safePath)
      val parentFile = archiveFile.getParentFile
      archiveFile.extractUncompress(parentFile)
    case _ ⇒
  }

  def fileSize(treeNodeData: TreeNodeData): Long = safePathToFile(treeNodeData.safePath).length

  def listFiles(tnd: TreeNodeData): Seq[TreeNodeData] = Utils.listFiles(tnd.safePath)

  def move(from: SafePath, to: SafePath): Unit = {
    val fromFile = safePathToFile(from)
    val toFile = safePathToFile(to)
    if (fromFile.exists && toFile.exists) {
      fromFile.move(new File(toFile, from.path.last))
    }
  }

  def renameFile(treeNodeData: TreeNodeData, name: String): TreeNodeData =
    renameFileFromPath(safePathToFile(treeNodeData.safePath), name)

  def renameKey(keyName: String, newName: String): Unit =
    Files.move(new File(Utils.authenticationKeysFile, keyName), new File(Utils.authenticationKeysFile, newName))

  def renameFileFromPath(filePath: SafePath, newName: String): TreeNodeData = {
    val targetFile = new File(filePath.parent, newName)

    Files.move(safePathToFile(filePath), targetFile, StandardCopyOption.REPLACE_EXISTING)
    TreeNodeData(newName, targetFile, false, 0L, "")

  }

  def saveFile(path: SafePath, fileContent: String): Unit = safePathToFile(path).content = fileContent

  def workspaceProjectNode(): SafePath = Utils.workspaceProjectFile

  def authenticationKeysPath(): SafePath = Utils.authenticationKeysFile

  // EXECUTIONS

  def allExecutionStates(): Seq[(ExecutionId, ExecutionInfo)] = execution.allStates

  def allStaticInfos(): Seq[(ExecutionId, StaticExecutionInfo)] = execution.allStaticInfos

  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)

  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def runScript(scriptData: ScriptData): Unit = {

    val execId = ExecutionId(getUUID)
    val script = safePathToFile(scriptData.scriptPath)
    val content = script.content

    execution.addStaticInfo(execId, StaticExecutionInfo(scriptData.scriptPath, content, System.currentTimeMillis()))

    def error(t: Throwable): Unit = execution.addError(execId, Failed(ErrorBuilder(t)))
    def message(message: String): Unit = execution.addError(execId, Failed(Error(message)))

    val project = new Project(script.getParentFileSafe)
    project.compile(script, Seq.empty) match {
      case ScriptFileDoesNotExists() ⇒ message("Script file does not exist")
      case CompilationError(e)       ⇒ error(e)
      case Compiled(compiled) ⇒
        Try(compiled.eval()) match {
          case Failure(e) ⇒ error(e)
          case Success(o) ⇒
            o match {
              case toPuzzle: PuzzleBuilder ⇒
                val puzzle = toPuzzle.buildPuzzle
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
                      case Success(ex) ⇒ execution.addDynamicInfo(execId, DynamicExecutionInfo(ex, outputStream))
                    }
                  case Failure(e) ⇒ error(e)
                }
              case _ ⇒ message("A puzzle have to be provided, the workflow can not be launched")
            }
        }
    }
  }

  def runningErrorEnvironmentAndOutputData(): (Seq[RunningEnvironmentData], Seq[RunningOutputData]) = atomic { implicit ctx ⇒
    val envIds = Runnings.ids
    (
      envIds.map {
        case (id, envIds) ⇒
          RunningEnvironmentData(id, Runnings.runningEnvironments(id).flatMap {
            _._2.environmentError
          })
      }.toSeq,
      envIds.keys.toSeq.map {
        Runnings.outputsDatas(_)
      }
    )
  }

}
