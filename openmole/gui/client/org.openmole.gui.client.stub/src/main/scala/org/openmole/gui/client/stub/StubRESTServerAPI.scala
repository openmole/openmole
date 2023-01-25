package org.openmole.gui.client.stub

/*
 * Copyright (C) 2023 Romain Reuillon
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

import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.shared.data.{TreeNodeData, *}
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.ServerAPI
import org.scalajs.dom.*

import scala.concurrent.duration.*
import scala.concurrent.Future

object AnimatedStubRESTServerAPI:
  case class MemoryFile(path: SafePath, content: String, time: Long = System.currentTimeMillis(), directory: Boolean = false)

  def apply() =
    val api = new AnimatedStubRESTServerAPI()

    val files = Seq(
      MemoryFile(SafePath("test.oms"), "val i = Val[Int]")
    )

    files.foreach(api.add)
    api

class AnimatedStubRESTServerAPI extends ServerAPI:
  import AnimatedStubRESTServerAPI.*

  val files = scala.collection.mutable.HashMap[SafePath, MemoryFile]()

  def add(file: MemoryFile) = files += file.path -> file

  override def size(safePath: SafePath): Future[Long] = Future.successful(0L)
  override def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean): Future[Seq[SafePath]] = Future.successful(Seq())

  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)] =
    val file = files(safePath)

    val res =
      hash match
        case None =>
          add(file.copy(content = content))
          (true, content)
        case Some(h) if overwrite == true || file.content.hashCode.toString == h =>
          add(file.copy(content = content))
          (true, content)
        case _ =>
          (false, file.content)

    Future.successful(res)

  override def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean] =
    val file = MemoryFile(path ++ name, "", directory = directory)
    add(file)
    Future.successful(true)

  override def extract(path: SafePath): Future[Option[ErrorData]] = Future.successful(None)

  override def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData] =
    val simpleFiles = files.values.filter(!_.directory)

    val fileData =
      simpleFiles.filter(_.path.parent == path).map { f =>
        TreeNodeData(
          name = f.path.name,
          size = f.content.size,
          time = f.time
        )
      }

    val directoryData =
      files.values.filter(_.directory).filter(_.path.startsWith(path)).map { d =>
        val isEmpty: Boolean = simpleFiles.forall(!_.path.startsWith(d.path))

        TreeNodeData(
          name = d.path.name,
          size = 0,
          time = d.time,
          directory = Some(TreeNodeData.Directory(isEmpty))
        )
      }

    Future.successful(directoryData.toSeq ++ fileData)


  override def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] = Future.successful(Seq.empty)
  override def move(from: SafePath, to: SafePath): Future[Unit] = Future.successful(())
  override def duplicate(path: SafePath, name: String): Future[SafePath] = Future.successful(SafePath.empty)

  override def deleteFiles(path: Seq[SafePath]): Future[Unit] =
    path.foreach(files.remove)
    Future.successful(())

  override def exists(path: SafePath): Future[Boolean] = Future.successful(false)
  override def temporaryDirectory(): Future[SafePath] = Future.successful(SafePath.empty)
  override def allStates(line: Int): Future[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = Future.successful((Seq.empty, Seq.empty))
  override def staticInfos(): Future[Seq[(ExecutionId, StaticExecutionInfo)]] = Future.successful(Seq.empty)
  override def cancelExecution(id: ExecutionId): Future[Unit] = Future.successful(())
  override def removeExecution(id: ExecutionId): Future[Unit] = Future.successful(())
  override def compileScript(script: ScriptData): Future[Option[ErrorData]] = Future.successful(None)
  override def runScript(script: ScriptData, validate: Boolean): Future[Unit] = Future.successful(())
  override def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit] = Future.successful(())
  override def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData] = Future.successful(EnvironmentErrorData.empty)
  override def listPlugins(): Future[Seq[Plugin]] = Future.successful(Seq.empty)
  override def guiPlugins(): Future[PluginExtensionData] = Future.successful(PluginExtensionData.empty)
  override def addPlugin(path: SafePath): Future[Seq[ErrorData]] = Future.successful(Seq.empty)
  override def removePlugin(path: SafePath): Future[Unit] = Future.successful(())
  override def findVisualisationPlugin(path: SafePath): Future[Option[GUIPluginAsJS]] = Future.successful(None)
  override def models(path: SafePath): Future[Seq[SafePath]] = Future.successful(Seq.empty)
  override def expandResources(resources: Resources): Future[Resources] = Future.successful(Resources.empty)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean): Future[Option[ErrorData]] = Future.successful(None)
  override def marketIndex(): Future[MarketIndex] = Future.successful(MarketIndex.empty)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Future[Unit] = Future.successful(())
  override def omSettings(): Future[OMSettings] = Future.successful(OMSettings(SafePath.empty, "stub", "stub", "0", true))
  override def shutdown(): Future[Unit] = Future.successful(())
  override def restart(): Future[Unit] = Future.successful(())
  override def isAlive(): Future[Boolean] = Future.successful(true)
  override def jvmInfos(): Future[JVMInfos] = Future.successful(JVMInfos("stub", "stub", 0, 0, 0))
  override def mdToHtml(safePath: SafePath): Future[String] = Future.successful("")
  override def sequence(safePath: SafePath): Future[SequenceData] = Future.successful(SequenceData.empty)

  override def upload(fileList: FileList, destinationPath: SafePath, fileTransferState: ProcessState ⇒ Unit, onLoadEnd: Seq[String] ⇒ Unit): Unit = {}

  override def download(safePath: SafePath, fileTransferState: ProcessState ⇒ Unit = _ ⇒ (), onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (), hash: Boolean = false): Unit =
    val content = files(safePath).content
    val h = if hash then Some(content.hashCode.toString) else None
    onLoadEnd(content, h)


class StubRESTServerAPI extends ServerAPI:
  override def size(safePath: SafePath): Future[Long] = Future.successful(0L)
  override def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean): Future[Seq[SafePath]] = Future.successful(Seq())
  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)] = Future.successful((true, ""))
  override def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean] = Future.successful(true)
  override def extract(path: SafePath): Future[Option[ErrorData]] = Future.successful(None)
  override def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData] = Future.successful(ListFilesData.empty)
  override def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] = Future.successful(Seq.empty)
  override def move(from: SafePath, to: SafePath): Future[Unit] = Future.successful(())
  override def duplicate(path: SafePath, name: String): Future[SafePath] = Future.successful(SafePath.empty)
  override def deleteFiles(path: Seq[SafePath]): Future[Unit] = Future.successful(())
  override def exists(path: SafePath): Future[Boolean] = Future.successful(false)
  override def temporaryDirectory(): Future[SafePath] = Future.successful(SafePath.empty)
  override def allStates(line: Int): Future[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = Future.successful((Seq.empty, Seq.empty))
  override def staticInfos(): Future[Seq[(ExecutionId, StaticExecutionInfo)]] = Future.successful(Seq.empty)
  override def cancelExecution(id: ExecutionId): Future[Unit] = Future.successful(())
  override def removeExecution(id: ExecutionId): Future[Unit] = Future.successful(())
  override def compileScript(script: ScriptData): Future[Option[ErrorData]] = Future.successful(None)
  override def runScript(script: ScriptData, validate: Boolean): Future[Unit] = Future.successful(())
  override def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit] = Future.successful(())
  override def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData] = Future.successful(EnvironmentErrorData.empty)
  override def listPlugins(): Future[Seq[Plugin]] = Future.successful(Seq.empty)
  override def guiPlugins(): Future[PluginExtensionData] = Future.successful(PluginExtensionData.empty)
  override def addPlugin(path: SafePath): Future[Seq[ErrorData]] = Future.successful(Seq.empty)
  override def removePlugin(path: SafePath): Future[Unit] = Future.successful(())
  override def findVisualisationPlugin(path: SafePath): Future[Option[GUIPluginAsJS]] = Future.successful(None)
  override def models(path: SafePath): Future[Seq[SafePath]] = Future.successful(Seq.empty)
  override def expandResources(resources: Resources): Future[Resources] = Future.successful(Resources.empty)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean): Future[Option[ErrorData]] = Future.successful(None)
  override def marketIndex(): Future[MarketIndex] = Future.successful(MarketIndex.empty)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Future[Unit] = Future.successful(())
  override def omSettings(): Future[OMSettings] = Future.successful(OMSettings(SafePath.empty, "stub", "stub", "0", true))
  override def shutdown(): Future[Unit] = Future.successful(())
  override def restart(): Future[Unit] = Future.successful(())
  override def isAlive(): Future[Boolean] = Future.successful(true)
  override def jvmInfos(): Future[JVMInfos] = Future.successful(JVMInfos("stub", "stub", 0, 0, 0))
  override def mdToHtml(safePath: SafePath): Future[String] = Future.successful("")
  override def sequence(safePath: SafePath): Future[SequenceData] = Future.successful(SequenceData.empty)


  override def upload(fileList: FileList, destinationPath: SafePath, fileTransferState: ProcessState ⇒ Unit, onLoadEnd: Seq[String] ⇒ Unit): Unit = {}
  override def download(safePath: SafePath, fileTransferState: ProcessState ⇒ Unit = _ ⇒ (), onLoadEnd: (String, Option[String]) ⇒ Unit = (_, _) ⇒ (), hash: Boolean = false): Unit = {}