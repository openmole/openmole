package org.openmole.gui.client.core

/*
 * Copyright (C) 2022 Romain Reuillon
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

class OpenMOLERESTServerAPI(fetch: Fetch) extends ServerAPI:
  override def size(safePath: SafePath) = fetch.future(_.size(safePath).future)
  override def copyFiles(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean) = fetch.future(_.copyFiles(safePaths, to, overwrite).future)
  override def saveFile(safePath: SafePath, content: String, hash: Option[String], overwrite: Boolean): Future[(Boolean, String)] = fetch.future(_.saveFile(safePath, content, hash, overwrite).future)
  override def createFile(path: SafePath, name: String, directory: Boolean): Future[Boolean] = fetch.future(_.createFile(path, name, directory).future)
  override def extract(path: SafePath): Future[Option[ErrorData]] = fetch.future(_.extract(path).future)
  override def listFiles(path: SafePath, filter: FileFilter): Future[ListFilesData] = fetch.future(_.listFiles(path, filter).future)
  override def listRecursive(path: SafePath, findString: Option[String]): Future[Seq[(SafePath, Boolean)]] = fetch.future(_.listRecursive(path, findString).future)
  override def move(from: SafePath, to: SafePath): Future[Unit] = fetch.future(_.move(from, to).future)
  override def duplicate(path: SafePath, name: String): Future[SafePath] = fetch.future(_.duplicate(path, name).future)
  override def deleteFiles(path: Seq[SafePath]): Future[Unit] = fetch.future(_.deleteFiles(path).future)
  override def exists(path: SafePath): Future[Boolean] = fetch.future(_.exists(path).future)
  override def temporaryDirectory(): Future[SafePath] = fetch.future(_.temporaryDirectory(()).future)
  override def allStates(line: Int): Future[(Seq[(ExecutionId, ExecutionInfo)], Seq[OutputStreamData])] = fetch.future(_.allStates(line).future)
  override def staticInfos(): Future[Seq[(ExecutionId, StaticExecutionInfo)]] = fetch.future(_.staticInfos(()).future)
  override def cancelExecution(id: ExecutionId): Future[Unit] = fetch.future(_.cancelExecution(id).future)
  override def removeExecution(id: ExecutionId): Future[Unit] = fetch.future(_.removeExecution(id).future)
  override def compileScript(script: ScriptData): Future[Option[ErrorData]] = fetch.future(_.compileScript(script).future, timeout = 120 seconds, warningTimeout = 60 seconds)
  override def runScript(script: ScriptData, validate: Boolean): Future[Unit] = fetch.future(_.runScript(script, validate).future, timeout = 120 seconds, warningTimeout = 60 seconds)
  override def clearEnvironmentErrors(environment: EnvironmentId): Future[Unit] = fetch.future(_.clearEnvironmentErrors(environment).future)
  override def runningErrorEnvironmentData(environment: EnvironmentId, lines: Int): Future[EnvironmentErrorData] = fetch.future(_.runningErrorEnvironmentData(environment, lines).future)
  override def listPlugins(): Future[Seq[Plugin]] = fetch.future(_.listPlugins(()).future)
  override def guiPlugins(): Future[PluginExtensionData] = fetch.future(_.guiPlugins(()).future)
  override def addPlugin(path: SafePath): Future[Seq[ErrorData]] = fetch.future(_.addPlugin(path).future)
  override def removePlugin(path: SafePath): Future[Unit] = fetch.future(_.removePlugin(path).future)
  override def findVisualisationPlugin(path: SafePath): Future[Option[GUIPluginAsJS]] = fetch.future(_.findVisualisationPlugin(path).future)
  override def models(path: SafePath): Future[Seq[SafePath]] = fetch.future(_.models(path).future)
  override def expandResources(resources: Resources): Future[Resources] = fetch.future(_.expandResources(resources).future)
  override def downloadHTTP(url: String, path: SafePath, extract: Boolean): Future[Option[ErrorData]] = fetch.future(_.downloadHTTP(url, path, extract).future)
  override def marketIndex(): Future[MarketIndex] = fetch.future(_.marketIndex(()).future)
  override def getMarketEntry(entry: MarketIndexEntry, safePath: SafePath): Future[Unit] = fetch.future(_.getMarketEntry(entry, safePath).future)
  override def omSettings(): Future[OMSettings] = fetch.future(_.omSettings(()).future)
  override def shutdown(): Future[Unit] = fetch.future(_.shutdown(()).future)
  override def restart(): Future[Unit] = fetch.future(_.restart(()).future)
  override def isAlive(): Future[Boolean] = fetch.future(_.isAlive(()).future, 3.seconds, 5.minutes)
  override def jvmInfos(): Future[JVMInfos] = fetch.future(_.jvmInfos(()).future)
  override def mdToHtml(safePath: SafePath): Future[String] = fetch.future(_.mdToHtml(safePath).future)
  override def sequence(safePath: SafePath): Future[SequenceData] = fetch.future(_.sequence(safePath).future)
