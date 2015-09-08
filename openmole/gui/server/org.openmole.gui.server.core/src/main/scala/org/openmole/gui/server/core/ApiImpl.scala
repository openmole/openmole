package org.openmole.gui.server.core

import java.net.URL
import java.util.zip.GZIPInputStream

import org.openmole.core.batch.environment.BatchEnvironment.{ EndUpload, BeginUpload, EndDownload, BeginDownload }
import org.openmole.core.buildinfo.MarketIndex
import org.openmole.core.event._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.pluginmanager._
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.workflow.execution.Environment
import org.openmole.core.workflow.execution.Environment.ExceptionRaised
import org.openmole.gui.misc.utils.Utils._
import org.openmole.gui.server.core.Runnings.RunningEnvironment
import org.openmole.gui.server.core.Utils._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.shared._
import org.openmole.gui.ext.data._
import java.io.{ InputStream, File }
import java.nio.file._
import org.openmole.console._
import org.osgi.framework.Bundle
import scala.util.{ Failure, Success, Try }
import org.openmole.console.ConsoleVariables
import org.openmole.core.workflow.mole.ExecutionContext
import org.openmole.core.workflow.puzzle.PuzzleBuilder
import org.openmole.tool.stream.StringPrintStream
import scala.concurrent.stm._
import org.openmole.tool.file._
import org.openmole.tool.tar._
import com.github.rjeschke._
import org.openmole.core.fileservice._
import org.openmole.core.buildinfo

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

  def deleteAuthenticationKey(keyName: String): Unit = authenticationFile(keyName).delete

  def deleteFile(safePath: SafePath): Unit = safePathToFile(safePath).recursiveDelete

  def extractTGZ(treeNodeData: TreeNodeData): Unit = treeNodeData.safePath.extension match {
    case FileExtension.TGZ ⇒
      val archiveFile = safePathToFile(treeNodeData.safePath)
      val parentFile = archiveFile.getParentFile
      archiveFile.extractUncompress(parentFile)
      parentFile.applyRecursive((f: File) ⇒ f.setWritable(true))
    case _ ⇒
  }

  def exists(safePath: SafePath): Boolean = safePathToFile(safePath).exists

  def fileSize(treeNodeData: TreeNodeData): Long = safePathToFile(treeNodeData.safePath).length

  def listFiles(tnd: TreeNodeData): Seq[TreeNodeData] = Utils.listFiles(tnd.safePath)

  def move(from: SafePath, to: SafePath): Unit = {
    val fromFile = safePathToFile(from)
    val toFile = safePathToFile(to)
    if (fromFile.exists && toFile.exists) {
      fromFile.move(new File(toFile, from.path.last))
    }
  }

  def mdToHtml(safePath: SafePath): String = safePath.extension match {
    case FileExtension.MD ⇒ MarkDownProcessor(safePathToFile(safePath).content)
    case _                ⇒ ""
  }

  def renameFile(treeNodeData: TreeNodeData, name: String): TreeNodeData =
    renameFileFromPath(safePathToFile(treeNodeData.safePath), name)

  def renameKey(keyName: String, newName: String): Unit =
    Files.move(authenticationFile(keyName), authenticationFile(newName), StandardCopyOption.REPLACE_EXISTING)

  def renameFileFromPath(filePath: SafePath, newName: String): TreeNodeData = {
    val targetFile = new File(filePath.parent, newName)

    Files.move(safePathToFile(filePath), targetFile, StandardCopyOption.REPLACE_EXISTING)
    TreeNodeData(newName, targetFile, false, false, 0L, "")

  }

  def saveFile(path: SafePath, fileContent: String): Unit = safePathToFile(path).content = fileContent

  def saveFiles(fileContents: Seq[AlterableFileContent]): Unit = fileContents.foreach { fc ⇒
    saveFile(fc.path, fc.content)
  }

  def workspacePath(): SafePath = Utils.workspaceProjectFile

  // EXECUTIONS

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
      case compiled: Compiled ⇒
        Try(compiled.eval()) match {
          case Failure(e) ⇒ error(e)
          case Success(o) ⇒
            val puzzle = o.buildPuzzle
            val outputStream = new StringPrintStream()

            val envIds = puzzle.environments.values.toSeq.map { env ⇒ EnvironmentId(getUUID) -> env }
            Runnings.add(execId, envIds, outputStream)

            envIds.foreach {
              case (envId, env) ⇒
                env.listen {
                  case (env, bdl: BeginDownload) ⇒ Runnings.update(envId) {
                    re ⇒ re.copy(networkActivity = re.networkActivity.copy(downloadingFiles = re.networkActivity.downloadingFiles + 1))
                  }
                  case (env, edl: EndDownload) ⇒ Runnings.update(envId) {
                    re ⇒
                      val size = re.networkActivity.downloadedSize + (if (edl.success) FileDecorator(edl.file).size else 0)
                      re.copy(networkActivity = re.networkActivity.copy(
                        downloadingFiles = re.networkActivity.downloadingFiles - 1,
                        downloadedSize = size,
                        readableDownloadedSize = readableByteCount(size))
                      )
                  }
                  case (env, bul: BeginUpload) ⇒ Runnings.update(envId) {
                    re ⇒ re.copy(networkActivity = re.networkActivity.copy(uploadingFiles = re.networkActivity.uploadingFiles + 1))
                  }
                  case (env, eul: EndUpload) ⇒ Runnings.update(envId) {
                    (re: RunningEnvironment) ⇒
                      val size = re.networkActivity.uploadedSize + (if (eul.success) FileDecorator(eul.file).size else 0)
                      re.copy(
                        networkActivity = re.networkActivity.copy(
                          uploadedSize = size,
                          readableUploadedSize = readableByteCount(size),
                          uploadingFiles = re.networkActivity.uploadingFiles - 1)
                      )
                  }
                }
            }
            Try(puzzle.toExecution(executionContext = ExecutionContext(out = outputStream))) match {
              case Success(ex) ⇒
                Try(ex.start) match {
                  case Failure(e) ⇒ error(e)
                  case Success(ex) ⇒
                    val inserted = execution.addDynamicInfo(execId, DynamicExecutionInfo(ex, outputStream))
                    if (!inserted) ex.cancel
                }
              case Failure(e) ⇒ error(e)
            }
        }
    }
  }

  def allStates() = execution.allStates

  def runningErrorEnvironmentAndOutputData(lines: Int, level: ErrorStateLevel): (Seq[RunningEnvironmentData], Seq[RunningOutputData]) = atomic { implicit ctx ⇒
    val envIds = Runnings.environmentIds
    (
      envIds.map {
        case (id, envIds) ⇒
          RunningEnvironmentData(
            id,
            Runnings.runningEnvironments(envIds).flatMap {
              case (envId, info) ⇒ info.environmentErrors(envId)
            }.filter {
              _.level == level
            }
          )
      }.toSeq,
      envIds.keys.toSeq.map {
        Runnings.outputsDatas(_, lines)
      }
    )
  }

  def buildInfo = buildinfo.info

  def marketIndex() = {
    def download[T](action: InputStream ⇒ T): T = {
      val url = new URL(buildinfo.marketAddress)
      val is = url.openStream()
      try action(is)
      finally is.close
    }

    def mapToMd(marketIndex: MarketIndex) =
      marketIndex.copy(entries = marketIndex.entries.map {
        e ⇒
          e.copy(readme = e.readme.map {
            MarkDownProcessor(_)
          })
      })

    if (!buildinfo.development) {
      val marketFile = (webUIProjectFile / s"market${buildinfo.version}.xml")
      marketFile.cache(f ⇒ download(_.copy(f)))
      mapToMd(SerialiserService.deserialise[buildinfo.MarketIndex](marketFile))
    }
    else mapToMd(download(SerialiserService.deserialise[buildinfo.MarketIndex](_)))
  }

  def getMarketEntry(entry: buildinfo.MarketIndexEntry, path: SafePath) = {
    val url = new URL(entry.url)
    val is = new TarInputStream(new GZIPInputStream(url.openStream()))
    try {
      is.extract(safePathToFile(path))
      autoAddPlugins(path)
    }
    finally is.close
  }

  //PLUGINS
  def addPlugin(path: SafePath): Unit = {
    val file = safePathToFile(path)
    addPlugins(PluginManager.plugins(file))
  }

  def autoAddPlugins(path: SafePath) = {
    val file = safePathToFile(path)

    def recurse(f: File): List[File] = {
      val subPlugins: List[File] = if (f.isDirectory) f.listFilesSafe.toList.flatMap(recurse) else Nil
      PluginManager.plugins(f).toList ::: subPlugins
    }

    addPlugins(recurse(file))
  }

  private def addPlugins(files: Iterable[File]): Unit = {
    val plugins =
      files.map { file ⇒
        val dest: File = Workspace.pluginDir / file.getName
        file copy dest
        dest
      }
    PluginManager.load(plugins)
  }

  def isPlugin(path: SafePath): Boolean = Utils.isPlugin(path)

  def listPlugins(): Iterable[Plugin] =
    Workspace.pluginDir.listFilesSafe.map(p ⇒ Plugin(p.getName))

  def removePlugin(plugin: Plugin): Unit = synchronized {
    val file = Workspace.pluginDir / plugin.name

    val allFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)
    for {
      b ← (file :: allFiles.toList).flatMap(PluginManager.bundle)
      if (b.getState == Bundle.ACTIVE)
    } b.uninstall()

    allFiles.foreach(_.recursiveDelete)
    file.recursiveDelete

    // FIXME: the bundles might not be fully unloaded, they might be dynamically imported by core.console
  }

}
