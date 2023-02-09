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
import org.openmole.gui.shared.api.*
import org.scalajs.dom.*

import java.util.UUID
import scala.concurrent.duration.*
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object AnimatedStubRESTServerAPI:
  case class MemoryFile(content: String, time: Long = System.currentTimeMillis(), directory: Boolean = false)

  def apply() =
    val api = new AnimatedStubRESTServerAPI()

    api.files ++= Seq(
      SafePath("test.oms") -> MemoryFile("val i = Val[Int]"),
      SafePath("test2.oms") -> MemoryFile("val j = Val[Double]")
    )

    api

class AnimatedStubRESTServerAPI extends ServerAPI:

  import AnimatedStubRESTServerAPI.*

  val files = scala.collection.mutable.HashMap[SafePath, MemoryFile]()
  val executions = scala.collection.mutable.HashMap[ExecutionId, ExecutionData]()
  val plugins = scala.collection.mutable.HashMap[SafePath, Plugin]()

  override def size(safePath: SafePath): Future[Long] =
    Future.successful(files(safePath).content.size)

  override def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean): Future[Seq[SafePath]] =
    val copies = paths.map((p, d) => d -> files(p))
    files ++= copies
    Future.successful(copies.map(_._1))

  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)] =
    val file = files(safePath)

    val res =
      hash match
        case None =>
          files += safePath -> file.copy(content = content)
          (true, content.hashCode.toString)
        case Some(h) if overwrite == true || file.content.hashCode.toString == h =>
          files += safePath -> file.copy(content = content)
          (true, content.hashCode.toString)
        case _ =>
          (false, file.content.hashCode.toString)

    Future.successful(res)

  override def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean] =
    files += (path ++ name) -> MemoryFile("", directory = directory)
    Future.successful(true)

  override def extract(path: SafePath): Future[Option[ErrorData]] = Future.successful(None)

  override def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData] =
    val simpleFiles = files.toSeq.filter(!_._2.directory)

    val fileData =
      simpleFiles.filter((p, _) => p.parent == path).map { (p, f) =>
        TreeNodeData(
          name = p.name,
          size = f.content.size,
          time = f.time
        )
      }

    val directoryData =
      files.toSeq.filter((p, f) => f.directory && p.startsWith(path)).map { (p, d) =>
        val isEmpty: Boolean = simpleFiles.forall((sp, _) => !sp.startsWith(p))

        TreeNodeData(
          name = p.name,
          size = 0,
          time = d.time,
          directory = Some(TreeNodeData.Directory(isEmpty))
        )
      }

    Future.successful(directoryData.toSeq ++ fileData)

  override def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] =
    def found = files.toSeq.filter { (f, _) => f.startsWith(path) && findString.map(s => f.name.contains(s)).getOrElse(true) }.map { (f, m) => f -> m.directory }

    Future.successful(found)

  override def move(from: SafePath, to: SafePath): Future[Unit] =
    val f = files.remove(from).get
    files += to -> f
    Future.successful(())

  override def deleteFiles(path: Seq[SafePath]): Future[Unit] =
    path.foreach(files.remove)
    Future.successful(())

  override def exists(path: SafePath): Future[Boolean] =
    Future.successful(files.contains(path))

  override def temporaryDirectory(): Future[SafePath] =
    Future.successful(SafePath("_tmp_"))

  override def executionState(line: Int, ids: Seq[ExecutionId]): Future[Seq[ExecutionData]] =
    val ex =
      ids match
        case Seq() => executions.values.toSeq
        case _ => ids.flatMap(executions.get)
    Future.successful(ex)


  override def cancelExecution(id: ExecutionId): Future[Unit] =
    val execution = executions(id)
    executions += id -> execution.copy(state = ExecutionState.Canceled(Seq(), Seq(), 1000L, true))
    Future.successful(())

  override def removeExecution(id: ExecutionId): Future[Unit] =
    executions -= id
    Future.successful(())

  override def compileScript(script: SafePath): Future[Option[ErrorData]] = Future.successful(None)

  override def launchScript(script: SafePath, validate: Boolean): Future[ExecutionId] =
    def capsules = Seq(ExecutionState.CapsuleExecution("stub", "stub", ExecutionState.JobStatuses(10, 10, 10), true))
    def environments = Seq(
      EnvironmentState(EnvironmentId(), "zebulon@iscpif.fr", 10, 10, 10, 10, NetworkActivity(), ExecutionActivity(1000), 10),
      EnvironmentState(EnvironmentId(), "egi", 5, 17, 10, 10, NetworkActivity(), ExecutionActivity(700), 20)
    )

    val id = ExecutionId()
    executions += id -> ExecutionData(id, script, files(script).content, System.currentTimeMillis(), ExecutionState.Finished(capsules, 1000L, environments, true), "stub output")
    Future.successful(id)

  override def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit] = Future.successful(())

  override def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData] = Future.successful(EnvironmentErrorData.empty)

  override def listPlugins(): Future[Seq[Plugin]] =
    Future.successful(plugins.values.toSeq)

  override def addPlugin(path: SafePath): Future[Seq[ErrorData]] =
    plugins += (path -> Plugin(path, System.currentTimeMillis().toString, true))
    Future.successful(Seq.empty)

  override def removePlugin(path: SafePath): Future[Unit] =
    plugins.remove(path)
    Future.successful(())

  override def omrMethod(path: SafePath): Future[String] = Future.successful("stub")

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

  override def fetchGUIPlugins(f: GUIPlugins ⇒ Unit) =
    import org.openmole.gui.plugin.authentication.sshlogin.*
    val authFact = Seq(new LoginAuthenticationFactory(using new LoginAuthenticationStubAPI())) //p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
    val wizardFactories = Seq()
    val analysisPlugin = Map[String, MethodAnalysisPlugin]()
    Future.successful(f(GUIPlugins(authFact, wizardFactories, analysisPlugin)))


