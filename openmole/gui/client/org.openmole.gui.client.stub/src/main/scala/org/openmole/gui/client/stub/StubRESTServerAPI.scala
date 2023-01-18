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
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*

import scala.concurrent.duration.*
import scala.concurrent.Future

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

