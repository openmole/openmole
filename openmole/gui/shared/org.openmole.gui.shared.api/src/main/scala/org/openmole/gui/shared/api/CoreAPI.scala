package org.openmole.gui.shared.api

/*
 * Copyright (C) 2025 Romain Reuillon
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


import sttp.tapir.*
import sttp.model.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import io.circe.generic.auto.*
import org.openmole.core.market.{MarketIndex, MarketIndexEntry}
import org.openmole.gui.shared.data.*

object CoreAPI:
  def prefix = "gui"

  // --------- Files ----------------

  lazy val size: TapirEndpoint[SafePath, Long] =
    endpoint.post
      .in(prefix / "file" / "size")
      .in(jsonBody[SafePath])
      .out(jsonBody[Long])
      .errorOut(jsonBody[ErrorData])

  lazy val saveFile: TapirEndpoint[(SafePath, String, Option[String], Boolean), (Boolean, String)] =
    endpoint.post
      .in(prefix / "file" / "save")
      .in(jsonBody[(SafePath, String, Option[String], Boolean)])
      .out(jsonBody[(Boolean, String)])
      .errorOut(jsonBody[ErrorData])

  lazy val copyFiles: TapirEndpoint[(Seq[(SafePath, SafePath)], Boolean), Seq[SafePath]] =
    endpoint.post
      .in(prefix / "file" / "copy")
      .in(jsonBody[(Seq[(SafePath, SafePath)], Boolean)])
      .out(jsonBody[Seq[SafePath]])
      .errorOut(jsonBody[ErrorData])

  lazy val createFile: TapirEndpoint[(SafePath, String, Boolean), Boolean] =
    endpoint.post
      .in(prefix / "file" / "create")
      .in(jsonBody[(SafePath, String, Boolean)])
      .out(jsonBody[Boolean])
      .errorOut(jsonBody[ErrorData])

  lazy val extractArchive: TapirEndpoint[(SafePath, SafePath), Unit] =
    endpoint.post
      .in(prefix / "file" / "extract-archive")
      .in(jsonBody[(SafePath, SafePath)])
      .errorOut(jsonBody[ErrorData])

  lazy val listFiles: TapirEndpoint[(SafePath, FileSorting, Boolean, Boolean), FileListData] =
    endpoint.post
      .in(prefix / "file" / "list")
      .in(jsonBody[(SafePath, FileSorting, Boolean, Boolean)])
      .out(jsonBody[FileListData])
      .errorOut(jsonBody[ErrorData])

  lazy val listRecursive: TapirEndpoint[(SafePath, Option[String], Boolean), Seq[(SafePath, Boolean)]] =
    endpoint.post
      .in(prefix / "file" / "list-recursive")
      .in(jsonBody[(SafePath, Option[String], Boolean)])
      .out(jsonBody[Seq[(SafePath, Boolean)]])
      .errorOut(jsonBody[ErrorData])

  lazy val move: TapirEndpoint[(Seq[(SafePath, SafePath)], Boolean), Seq[SafePath]] =
    endpoint.post
      .in(prefix / "file" / "move")
      .in(jsonBody[(Seq[(SafePath, SafePath)], Boolean)])
      .out(jsonBody[Seq[SafePath]])
      .errorOut(jsonBody[ErrorData])

  lazy val duplicate: TapirEndpoint[(SafePath, String), SafePath] =
    endpoint.post
      .in(prefix / "file" / "duplicate")
      .in(jsonBody[(SafePath, String)])
      .out(jsonBody[SafePath])
      .errorOut(jsonBody[ErrorData])

  lazy val deleteFiles: TapirEndpoint[Seq[SafePath], Unit] =
    endpoint.post
      .in(prefix / "file" / "delete")
      .in(jsonBody[Seq[SafePath]])
      .errorOut(jsonBody[ErrorData])

  lazy val exists: TapirEndpoint[SafePath, Boolean] =
    endpoint.post
      .in(prefix / "file" / "exists")
      .in(jsonBody[SafePath])
      .out(jsonBody[Boolean])
      .errorOut(jsonBody[ErrorData])

  lazy val isText: TapirEndpoint[SafePath, Boolean] =
    endpoint.post
      .in(prefix / "file" / "is-text")
      .in(jsonBody[SafePath])
      .out(jsonBody[Boolean])
      .errorOut(jsonBody[ErrorData])

  lazy val temporaryDirectory: TapirEndpoint[Unit, SafePath] =
    endpoint.get
      .in(prefix / "file" / "temporary-directory")
      .out(jsonBody[SafePath])
      .errorOut(jsonBody[ErrorData])

  lazy val omrMethod: TapirEndpoint[SafePath, Option[String]] =
    endpoint.post
      .in(prefix / "file" / "omr" / "method")
      .in(jsonBody[SafePath])
      .out(jsonBody[Option[String]])
      .errorOut(jsonBody[ErrorData])

  lazy val omrContent: TapirEndpoint[(SafePath, Option[String]), GUIOMRContent] =
    endpoint.post
      .in(prefix / "file" / "omr" / "content")
      .in(jsonBody[(SafePath, Option[String])])
      .out(jsonBody[GUIOMRContent])
      .errorOut(jsonBody[ErrorData])

  lazy val omrFiles: TapirEndpoint[SafePath, Option[SafePath]] =
    endpoint.post
      .in(prefix / "file" / "omr" / "files")
      .in(jsonBody[SafePath])
      .out(jsonBody[Option[SafePath]])
      .errorOut(jsonBody[ErrorData])

  lazy val omrDataIndex: TapirEndpoint[SafePath, Seq[GUIOMRDataIndex]] =
    endpoint.post
      .in(prefix / "file" / "omr" / "index")
      .in(jsonBody[SafePath])
      .out(jsonBody[Seq[GUIOMRDataIndex]])
      .errorOut(jsonBody[ErrorData])

  lazy val cloneRepository: TapirEndpoint[(String, SafePath, Boolean), Option[SafePath]] =
    endpoint.post
      .in(prefix / "file" / "git" / "clone")
      .in(jsonBody[(String, SafePath, Boolean)])
      .out(jsonBody[Option[SafePath]])
      .errorOut(jsonBody[ErrorData])

  lazy val commit: TapirEndpoint[(Seq[SafePath], String), Unit] =
    endpoint.post
      .in(prefix / "file" / "git" / "commit")
      .in(jsonBody[(Seq[SafePath], String)])
      .out(jsonBody[Unit])
      .errorOut(jsonBody[ErrorData])

  lazy val revert: TapirEndpoint[Seq[SafePath], Unit] =
    endpoint.post
      .in(prefix / "file" / "git" / "revert")
      .in(jsonBody[Seq[SafePath]])
      .out(jsonBody[Unit])
      .errorOut(jsonBody[ErrorData])

  lazy val add: TapirEndpoint[Seq[SafePath], Unit] =
    endpoint.post
      .in(prefix / "file" / "git" / "add")
      .in(jsonBody[Seq[SafePath]])
      .out(jsonBody[Unit])
      .errorOut(jsonBody[ErrorData])

  lazy val pull: TapirEndpoint[SafePath, MergeStatus] =
    endpoint.post
      .in(prefix / "file" / "git" / "pull")
      .in(jsonBody[SafePath])
      .out(jsonBody[MergeStatus])
      .errorOut(jsonBody[ErrorData])

  lazy val push: TapirEndpoint[SafePath, PushStatus] =
    endpoint.post
      .in(prefix / "git" / "push")
      .in(jsonBody[SafePath])
      .out(jsonBody[PushStatus])
      .errorOut(jsonBody[ErrorData])

  lazy val branchList: TapirEndpoint[SafePath, Option[BranchData]] =
    endpoint.post
      .in(prefix / "file" / "git" / "branch-list")
      .in(jsonBody[SafePath])
      .out(jsonBody[Option[BranchData]])
      .errorOut(jsonBody[ErrorData])

  lazy val checkout: TapirEndpoint[(SafePath, String), Unit] =
    endpoint.post
      .in(prefix / "file" / "git" / "checkout")
      .in(jsonBody[(SafePath, String)])
      .errorOut(jsonBody[ErrorData])

  lazy val stash: TapirEndpoint[SafePath, Unit] =
    endpoint.post
      .in(prefix / "file" / "git" / "stash")
      .in(jsonBody[SafePath])
      .errorOut(jsonBody[ErrorData])

  lazy val stashPop: TapirEndpoint[SafePath, MergeStatus] =
    endpoint.post
      .in(prefix / "file" / "git" / "stash-pop")
      .in(jsonBody[SafePath])
      .out(jsonBody[MergeStatus])
      .errorOut(jsonBody[ErrorData])


  lazy val gitAuthentications: TapirEndpoint[Unit, Seq[GitPrivateKeyAuthenticationData]] =
    endpoint.get
      .in(prefix / "git" / "authentications")
      .out(jsonBody[Seq[GitPrivateKeyAuthenticationData]])
      .errorOut(jsonBody[ErrorData])

  lazy val addGitAuthentication: TapirEndpoint[GitPrivateKeyAuthenticationData, Unit] =
    endpoint.post
      .in(prefix / "git" / "add-authentication")
      .in(jsonBody[GitPrivateKeyAuthenticationData])
      .errorOut(jsonBody[ErrorData])

  lazy val removeGitAuthentication: TapirEndpoint[(GitPrivateKeyAuthenticationData, Boolean), Unit] =
    endpoint.post
      .in(prefix / "git" / "remove-authentication")
      .in(jsonBody[(GitPrivateKeyAuthenticationData, Boolean)])
      .errorOut(jsonBody[ErrorData])

  lazy val testGitAuthentication: TapirEndpoint[GitPrivateKeyAuthenticationData, Seq[Test]] =
    endpoint.post
      .in(prefix / "git" / "test-authentication")
      .in(jsonBody[GitPrivateKeyAuthenticationData])
      .out(jsonBody[Seq[Test]])
      .errorOut(jsonBody[ErrorData])

  // ---------- Executions --------------------

  lazy val executionState: TapirEndpoint[Seq[ExecutionId], Seq[ExecutionData]] =
    endpoint.post
      .in(prefix / "execution" / "state")
      .in(jsonBody[Seq[ExecutionId]])
      .out(jsonBody[Seq[ExecutionData]])
      .errorOut(jsonBody[ErrorData])

  lazy val executionOutput: TapirEndpoint[(ExecutionId, Int), ExecutionOutput] =
    endpoint.post
      .in(prefix / "execution" / "output")
      .in(jsonBody[(ExecutionId, Int)])
      .out(jsonBody[ExecutionOutput])
      .errorOut(jsonBody[ErrorData])

  lazy val cancelExecution: TapirEndpoint[ExecutionId, Unit] =
    endpoint.post
      .in(prefix / "execution" / "cancel")
      .in(jsonBody[ExecutionId])
      .errorOut(jsonBody[ErrorData])

  lazy val removeExecution: TapirEndpoint[ExecutionId, Unit] =
    endpoint.post
      .in(prefix / "execution" / "remove")
      .in(jsonBody[ExecutionId])
      .errorOut(jsonBody[ErrorData])

  lazy val validateScript: TapirEndpoint[SafePath, Option[ErrorData]] =
    endpoint.post
      .in(prefix / "execution" / "compile")
      .in(jsonBody[SafePath])
      .out(jsonBody[Option[ErrorData]])
      .errorOut(jsonBody[ErrorData])

  lazy val launchScript: TapirEndpoint[(SafePath, Boolean), ExecutionId] =
    endpoint.post
      .in(prefix / "execution" / "launch")
      .in(jsonBody[(SafePath, Boolean)])
      .out(jsonBody[ExecutionId])
      .errorOut(jsonBody[ErrorData])

  lazy val clearEnvironmentErrors: TapirEndpoint[(ExecutionId, EnvironmentId), Unit] =
    endpoint.post
      .in(prefix / "execution" / "clear-environment-error")
      .in(jsonBody[(ExecutionId, EnvironmentId)])
      .errorOut(jsonBody[ErrorData])

  lazy val listEnvironmentErrors: TapirEndpoint[(ExecutionId, EnvironmentId, Int), Seq[EnvironmentError]] =
    endpoint.post
      .in(prefix / "execution" / "list-environment-error")
      .in(jsonBody[(ExecutionId, EnvironmentId, Int)])
      .out(jsonBody[Seq[EnvironmentError]])
      .errorOut(jsonBody[ErrorData])

  // ---- Plugins -----
  lazy val listPlugins: TapirEndpoint[Unit, Seq[Plugin]] =
    endpoint.get
      .in(prefix / "plugin" / "list")
      .out(jsonBody[Seq[Plugin]])
      .errorOut(jsonBody[ErrorData])

  lazy val guiPlugins: TapirEndpoint[Unit, PluginExtensionData] =
    endpoint.get
      .in(prefix / "plugin" / "gui")
      .out(jsonBody[PluginExtensionData])
      .errorOut(jsonBody[ErrorData])

  lazy val addPlugin: TapirEndpoint[SafePath, Seq[ErrorData]] =
    endpoint.post
      .in(prefix / "plugin" / "add")
      .in(jsonBody[SafePath])
      .out(jsonBody[Seq[ErrorData]])
      .errorOut(jsonBody[ErrorData])

  lazy val removePlugin: TapirEndpoint[SafePath, Unit] =
    endpoint.post
      .in(prefix / "plugin" / "remove")
      .in(jsonBody[SafePath])
      .errorOut(jsonBody[ErrorData])

  // ---- Model Wizards --------------

  lazy val downloadHTTP: TapirEndpoint[(String, SafePath, Boolean, Boolean), Unit] =
    endpoint.post
      .in(prefix / "wizard" / "download-http")
      .in(jsonBody[(String, SafePath, Boolean, Boolean)])
      .errorOut(jsonBody[ErrorData])

  // ---------- Market ----------

  lazy val marketIndex: TapirEndpoint[Unit, MarketIndex] =
    endpoint.get
      .in(prefix / "market" / "index")
      .out(jsonBody[MarketIndex])
      .errorOut(jsonBody[ErrorData])

  lazy val getMarketEntry: TapirEndpoint[(MarketIndexEntry, SafePath), Unit] =
    endpoint.post
      .in(prefix / "market" / "get-entry")
      .in(jsonBody[(MarketIndexEntry, SafePath)])
      .errorOut(jsonBody[ErrorData])

  // --------- Application ---------------
  lazy val omSettings: TapirEndpoint[Unit, OMSettings] =
    endpoint.get
      .in(prefix / "application" / "settings")
      .out(jsonBody[OMSettings])
      .errorOut(jsonBody[ErrorData])

  lazy val shutdown: TapirEndpoint[Unit, Unit] =
    endpoint.get
      .in(prefix / prefix / "application" / "shutdown")
      .errorOut(jsonBody[ErrorData])

  lazy val isAlive: TapirEndpoint[Unit, Boolean] =
    endpoint.get
      .in(prefix / "application" / "is-alive")
      .out(jsonBody[Boolean])
      .errorOut(jsonBody[ErrorData])
  
  lazy val jvmInfos: TapirEndpoint[Unit, JVMInfos] =
    endpoint.get
      .in(prefix / "application" / "jvm-infos")
      .out(jsonBody[JVMInfos])
      .errorOut(jsonBody[ErrorData])

  lazy val listNotification: TapirEndpoint[Unit, Seq[NotificationEvent]] =
    endpoint.post
      .in(prefix / "application" / "list-notification")
      .out(jsonBody[Seq[NotificationEvent]])
      .errorOut(jsonBody[ErrorData])

  lazy val clearNotification: TapirEndpoint[Seq[Long], Unit] =
    endpoint.post
      .in(prefix / "application" / "clear-notification")
      .in(jsonBody[Seq[Long]])
      .errorOut(jsonBody[ErrorData])

  lazy val removeContainerCache: TapirEndpoint[Unit, Unit] =
    endpoint.post
      .in(prefix / "application" / "remove-container-cache")
      .errorOut(jsonBody[ErrorData])

  lazy val mdToHtml: TapirEndpoint[SafePath, String] =
    endpoint.post
      .in(prefix / "tool" / "md-to-html")
      .in(jsonBody[SafePath])
      .out(jsonBody[String])
      .errorOut(jsonBody[ErrorData])

  // ------------ Tools --------------------
  lazy val sequence: TapirEndpoint[SafePath, SequenceData] =
    endpoint.post
      .in(prefix / "tool" / "sequence")
      .in(jsonBody[SafePath])
      .out(jsonBody[SequenceData])
      .errorOut(jsonBody[ErrorData])
