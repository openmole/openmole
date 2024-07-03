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
import org.openmole.gui.client
import org.openmole.gui.shared.data.{TreeNodeData, *}
import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.NotificationEvent.id
import org.scalajs.dom.*

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.duration.*
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

object AnimatedStubRESTServerAPI:
  case class MemoryFile(content: String, time: Long = System.currentTimeMillis(), directory: Boolean = false, compilation: Option[CompilationErrorData] = None)

  def SafePath(value: String*): SafePath = org.openmole.gui.shared.data.SafePath(value, ServerFileSystemContext.Project)


  def apply() =
    val api = new AnimatedStubRESTServerAPI()

    def bigDirectory =
      Seq(SafePath("big") -> MemoryFile("", directory = true)) ++ (0 until 2000).map { i => SafePath("big", i.toString) -> MemoryFile(i.toString) }

    def directory = Seq(SafePath("directory") -> MemoryFile("", directory = true), SafePath("directory", "file.txt") -> MemoryFile("text"))

    api.files ++= Seq(
      SafePath("testLongLongLongLongLongLongLongLongLongLongLongLongLongLong.oms") -> MemoryFile("val i = Val[Int]"),
      SafePath("test2.oms") -> MemoryFile(
        """val j = Val[Double]
          |val i = Val[error]
          |""".stripMargin,
        compilation = Some(CompilationErrorData(Seq(ScriptError("stub", Some(ScriptError.Position(2, 0, 0, 0)))), "stub"))
      ),
      SafePath("file.txt") -> MemoryFile("""modify me if you can!""")
    )

    api.files ++= bigDirectory ++ directory

    api

class AnimatedStubRESTServerAPI extends ServerAPI:

  import AnimatedStubRESTServerAPI.*

  val files = scala.collection.mutable.HashMap[SafePath, MemoryFile]()
  val executions = scala.collection.mutable.HashMap[ExecutionId, ExecutionData]()
  val plugins = scala.collection.mutable.HashMap[SafePath, Plugin]()
  val notification = scala.collection.mutable.ListBuffer[NotificationEvent]()
  val notificationId = new AtomicLong()

  override def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean)(using BasePath): Future[Seq[SafePath]] =
    val copies = paths.map((p, d) => d -> files(p))
    files ++= copies
    Future.successful(copies.map(_._1))

  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean)(using BasePath): Future[(Boolean, String)] =
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

  override def createFile(path: SafePath, name: String, directory: Boolean)(using BasePath): Future[Boolean] =
    files += (path ++ name) -> MemoryFile("", directory = directory)
    Future.successful(true)

  override def extractArchive(path: SafePath, to: SafePath)(using BasePath): Future[Unit] = Future.successful(())

  override def listFiles(path: SafePath, filter: FileSorting, withHidden: Boolean)(using BasePath): Future[FileListData] =
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
      files.toSeq.filter((p, f) => f.directory && p.parent == path).map { (p, d) =>
        val isEmpty: Boolean = simpleFiles.forall((sp, _) => !sp.startsWith(p))

        TreeNodeData(
          name = p.name,
          size = 0,
          time = d.time,
          directory = Some(TreeNodeData.Directory(isEmpty))
        )
      }

    val sorted = (directoryData ++ fileData).sorted(FileSorting.toOrdering(filter))
    def taken =
      filter.size match
        case Some(n) => FileListData(sorted.take(n), n, sorted.size)
        case None => FileListData(sorted, sorted.size, sorted.size)

    Future.successful(taken)

  override def listRecursive(path: SafePath, findString: Option[String], withHidden: Boolean)(using BasePath): Future[Seq[(SafePath, Boolean)]] =
    def found = files.toSeq.filter { (f, _) => f.startsWith(path) && findString.map(s => f.name.contains(s)).getOrElse(true) }.map { (f, m) => f -> m.directory }

    Future.successful(found)

  override def move(paths: Seq[(SafePath, SafePath)])(using BasePath): Future[Unit] =
    paths.foreach { (from, to) =>
      val f = files.remove(from).get
      files += to -> f
    }
    Future.successful(())

  override def deleteFiles(path: Seq[SafePath])(using BasePath): Future[Unit] =
    path.foreach(files.remove)
    Future.successful(())

  override def exists(path: SafePath)(using BasePath): Future[Boolean] =
    Future.successful(files.contains(path))

  override def isTextFile(path: SafePath)(using BasePath): Future[Boolean] =
    Future.successful(true)

  override def temporaryDirectory()(using BasePath): Future[SafePath] =
    Future.successful(SafePath("_tmp_"))

  override def executionState(ids: Seq[ExecutionId])(using BasePath): Future[Seq[ExecutionData]] =
    val ex =
      ids match
        case Seq() => executions.values.toSeq
        case _ => ids.flatMap(executions.get)
    Future.successful(ex)

  override def executionOutput(id: ExecutionId, lines: Int)(using BasePath) =
    val output = "stub output\n" * 10000
    val res = output.split("\n").takeRight(lines)

    Future.successful(ExecutionOutput(res.mkString("\n"), res.length, output.split("\n").length))

  override def cancelExecution(id: ExecutionId)(using BasePath): Future[Unit] =
    val execution = executions(id)
    executions += id -> execution.copy(state = ExecutionState.Canceled(Seq(), Seq(), 1000L, true))
    Future.successful(())

  override def removeExecution(id: ExecutionId)(using BasePath): Future[Unit] =
    executions -= id
    Future.successful(())

  override def validateScript(script: SafePath)(using BasePath): Future[Option[ErrorData]] =
    Future.successful(files.get(script).flatMap(_.compilation))

  override def launchScript(script: SafePath, validate: Boolean)(using BasePath): Future[ExecutionId] =
    def capsules = Seq(ExecutionState.CapsuleExecution("stub", "stub", ExecutionState.JobStatuses(10, 10, 10), true, 1))

    def environments = Seq(
      EnvironmentState(EnvironmentId(), "zebulon@iscpif.fr", 10, 10, 10, 10, NetworkActivity(), ExecutionActivity(1000), 10),
      EnvironmentState(EnvironmentId(), "egi", 5, 17, 10, 10, NetworkActivity(), ExecutionActivity(700), 20)
    )

    val id = ExecutionId()
    executions += id -> ExecutionData(id, script, files(script).content, System.currentTimeMillis(), ExecutionState.Finished(capsules, 1000L, environments, true), 10000L)
    notification += NotificationEvent.MoleExecutionFinished(id, script, None, System.currentTimeMillis(),  notificationId.getAndIncrement())

    Future.successful(id)

  override def clearEnvironmentError(executionId: ExecutionId, environment: EnvironmentId)(using BasePath): Future[Unit] = Future.successful(())

  override def listEnvironmentError(executionId: ExecutionId, environment: EnvironmentId, lines: Int)(using BasePath): Future[Seq[EnvironmentError]] =
    Future.successful(
      Seq(
        EnvironmentError(environment, Some("Something is wrong"), ErrorData("blablab"), 0L, ErrorStateLevel.Error),
        EnvironmentError(environment, Some("Something is also wrong"), ErrorData("blablab"), 100L, ErrorStateLevel.Error),
        EnvironmentError(environment, Some("Something is still wrong"), ErrorData("blablab"), 1000L, ErrorStateLevel.Debug),
        EnvironmentError(environment, Some("Something is wrong, check that"), ErrorData("blablab"), 100000L, ErrorStateLevel.Debug)
      )
    )

  override def listPlugins()(using BasePath): Future[Seq[Plugin]] =
    Future.successful(plugins.values.toSeq)

  override def addPlugin(path: SafePath)(using BasePath): Future[Seq[ErrorData]] =
    plugins += (path -> Plugin(path, System.currentTimeMillis(), true))
    Future.successful(Seq.empty)

  override def removePlugin(path: SafePath)(using BasePath): Future[Unit] =
    plugins.remove(path)
    Future.successful(())

  override def omrMethod(path: SafePath)(using BasePath): Future[Option[String]] = Future.successful(None)

  override def omrContent(path: SafePath, d: Option[String])(using BasePath): Future[GUIOMRContent] = Future.successful(
    GUIOMRContent(
      section = Seq(),
      openMoleVersion = "stub",
      executionId = "stub",
      script = None,
      timeStart = System.currentTimeMillis(),
      timeSave = System.currentTimeMillis(),
      index = None
    )
  )

  override def omrFiles(path: SafePath)(using BasePath): Future[Option[SafePath]] = Future.successful(None)
  override def omrDataIndex(path: SafePath)(using BasePath): Future[Seq[GUIOMRDataIndex]] = Future.successful(Seq())

  override def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean)(using BasePath): Future[Unit] = Future.successful(None)

  override def marketIndex()(using BasePath): Future[MarketIndex] =
    def marketIndex =
      MarketIndex(
        entries = Seq(
          MarketIndexEntry("Beautiful R", "archive", Some("\n#R\n\nThis workflow execute 100 times a R program with differents inputs. The R task computes a matrix multiplication.\n"), Seq("R", "Stats")),
          MarketIndexEntry("Python in action", "archive", Some("# Python"), Seq("Python", "simulation")),
          MarketIndexEntry("Scala for newbies", "archive", Some("# Scala\n## Introduction"), Seq("Scala", "simulation")),
          MarketIndexEntry("Calibrate your Netlogo", "archive", Some("# Netlogo"), Seq("Python", "simulation"))
        )
      )

    Future.successful(marketIndex)

  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath)(using BasePath): Future[Unit] = Future.successful(())

  override def omSettings()(using BasePath): Future[OMSettings] = Future.successful(OMSettings(org.openmole.gui.shared.data.SafePath.root(ServerFileSystemContext.Project), "stub", "stub", System.currentTimeMillis(), true))

  override def shutdown()(using BasePath): Future[Unit] = Future.successful(())

  override def restart()(using BasePath): Future[Unit] = Future.successful(())

  override def isAlive()(using BasePath): Future[Boolean] = Future.successful(true)

  override def jvmInfos()(using BasePath): Future[JVMInfos] = Future.successful(JVMInfos("stub", "stub", 0, 0, 0))

  override def mdToHtml(safePath: SafePath)(using BasePath): Future[String] = Future.successful("")

  override def sequence(safePath: SafePath)(using BasePath): Future[SequenceData] = Future.successful(SequenceData.empty)

  override def upload(files: Seq[(File, SafePath)], fileTransferState: ProcessState ⇒ Unit)(using BasePath): Future[Seq[(RelativePath, SafePath)]] = Future.successful(Seq())

  override def download(safePath: SafePath, fileTransferState: ProcessState ⇒ Unit = _ ⇒ (), hash: Boolean = false)(using BasePath): Future[(String, Option[String])] =
    val content = files(safePath).content
    val h = if hash then Some(content.hashCode.toString) else None
    Future.successful((content, h))

  override def fetchGUIPlugins(f: GUIPlugins ⇒ Unit)(using BasePath) =
    import org.openmole.gui.plugin.authentication.sshlogin.*
    val authFact = Seq(new LoginAuthenticationFactory(using new LoginAuthenticationStubAPI())) //p.authentications.map { gp ⇒ Plugins.buildJSObject[AuthenticationPluginFactory](gp) }
    val wizardFactories = Seq()
    val analysisPlugin = Map[String, MethodAnalysisPlugin]()
    Future.successful(f(client.ext.GUIPlugins(authFact, wizardFactories, analysisPlugin)))

  override def listNotification()(using BasePath): Future[Seq[NotificationEvent]] = Future.successful(notification.toSeq)

  override def clearNotification(ids: Seq[Long])(using BasePath): Future[Unit] =
    val kept = notification.filterNot(n => ids.contains(NotificationEvent.id(n)))
    notification.clear()
    notification.addAll(kept)
    Future.successful(())

  override def removeContainerCache()(using BasePath): Future[Unit] = Future.successful(())

