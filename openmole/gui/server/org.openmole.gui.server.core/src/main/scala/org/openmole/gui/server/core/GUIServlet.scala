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
package org.openmole.gui.server.core

import java.nio.ByteBuffer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scalatags.Text.all._
import scalatags.Text.{ all ⇒ tags }
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{ FileService, FileServiceCache }
import org.openmole.core.preference.Preference
import org.openmole.core.replication.ReplicaCatalog
import org.openmole.core.serializer.SerializerService
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.networkservice._
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.gui.shared.data.*
import org.openmole.gui.server.ext.{OMRouter, utils}
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.file.*
import org.openmole.tool.lock.LockRepository
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.{ RandomProvider, Seeder }
import org.openmole.tool.stream._
import org.openmole.tool.archive._

import scala.util.{ Failure, Success, Try }

object GUIServerServices {

  case class ServicesProvider(guiServices: GUIServerServices) extends Services:
    implicit def services: GUIServerServices = guiServices
    export guiServices.*

  def apply(workspace: Workspace, httpProxy: Option[String], logLevel: Option[Level], logFileLevel: Option[Level], password: Option[String]) =
    implicit val ws: Workspace = workspace
    implicit val preference: Preference = org.openmole.core.services.Services.preference(ws)
    implicit val newFile: TmpDirectory = TmpDirectory(workspace)
    implicit val seeder: Seeder = Seeder()
    implicit val serializerService: SerializerService = SerializerService()
    implicit val threadProvider: ThreadProvider = ThreadProvider()
    implicit val authenticationStore: AuthenticationStore = AuthenticationStore(ws)
    implicit val fileService: FileService = FileService()
    implicit val randomProvider: RandomProvider = RandomProvider(seeder.newRNG)
    implicit val eventDispatcher: EventDispatcher = EventDispatcher()
    implicit val outputRedirection: OutputRedirection = OutputRedirection()
    implicit val networkService: NetworkService = NetworkService(httpProxy)
    implicit val fileServiceCache: FileServiceCache = FileServiceCache()
    implicit val replicaCatalog: ReplicaCatalog = ReplicaCatalog(ws)
    implicit val loggerService: LoggerService = LoggerService(logLevel, file = Some(workspace.location / Workspace.logLocation), fileLevel = logFileLevel)
    implicit val timeService: TimeService = TimeService()
    given Cypher = Cypher(password)

    new GUIServerServices()

  def dispose(services: GUIServerServices) =
    scala.util.Try(Workspace.clean(services.workspace))
    scala.util.Try(services.threadProvider.stop())

  def withServices[T](workspace: Workspace, httpProxy: Option[String], logLevel: Option[Level], logFileLevel: Option[Level], password: Option[String])(f: GUIServerServices ⇒ T) =
    val services = GUIServerServices(workspace, httpProxy, logLevel, logFileLevel, password)
    try f(services)
    finally dispose(services)

}

class GUIServerServices(implicit
  val workspace:           Workspace,
  val preference:          Preference,
  val threadProvider:      ThreadProvider,
  val seeder:              Seeder,
  val replicaCatalog:      ReplicaCatalog,
  val tmpDirectory:        TmpDirectory,
  val authenticationStore: AuthenticationStore,
  val serializerService:   SerializerService,
  val fileService:         FileService,
  val fileServiceCache:    FileServiceCache,
  val randomProvider:      RandomProvider,
  val eventDispatcher:     EventDispatcher,
  val outputRedirection:   OutputRedirection,
  val networkService:      NetworkService,
  val loggerService:       LoggerService,
  val timeService:         TimeService,
  val cypher:              Cypher
)

object GUIServlet:

  def html(javascriptMethod: String, cssFiles: Seq[String], extraHeader: String) = tags.html(
    tags.head(
      tags.link(tags.rel := "icon", tags.href := "img/favicon.svg", tags.`type` := "img/svg+xml"),
      tags.meta(tags.httpEquiv := "content-type", tags.content := "text/html; charset=UTF-8"),
      cssFiles.map { f ⇒ tags.link(tags.rel := "stylesheet", tags.`type` := "text/css", href := f) },
      tags.script(tags.`type` := "text/javascript", tags.src := "js/plotly.min.js"),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/ace.js"),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/nouislider.min.js"),
      tags.script(tags.`type` := "text/javascript", tags.src := "js/openmole-webpacked.js"),
      //tags.script(tags.`type` := "text/javascript", tags.src := "js/" + utils.githubTheme),
      tags.link(tags.rel := "stylesheet", href := "css/bootstrap-icons/font/bootstrap-icons.min.css"),
      RawFrag(extraHeader)
    ),
    tags.body(
      tags.div(id := "openmole-content"),
      tags.script(javascriptMethod)
    )
  )

  // Get all the css files in the workspace (it is not working with js because of the order)
  def cssFiles(webapp: File) = (webapp / "css").listFilesSafe.map { f => s"css/${f.getName}" }.sorted.toSeq

  val webpackLibrary = "openmole_library.openmole_library"


