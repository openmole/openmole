package org.openmole.gui.server.core

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

import cats.effect.*
import endpoints4s.http4s.server
import org.http4s
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workspace.Workspace
import org.openmole.gui.server.ext
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.server.core.{ApiImpl, GUIServerServices}
import org.openmole.gui.shared.api
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*

object CoreAPIServer:
  def download(req: Request[IO], safePath: SafePath, name: Option[String] = None, topDirectory: Boolean = true)(using Workspace) =
    val f = safePathToFile(safePath)

    val response =
      if !f.exists()
      then Status.NotFound.apply(s"The file ${safePath.path.mkString} does not exist.")
      else if f.isDirectory
      then CoreAPIServer.directoryDownload(f, name, topDirectory = topDirectory)
      else
        import org.openmole.core.format.*
        if !OMRFormat.isOMR(f)
        then CoreAPIServer.fileDownload(f, req, name)
        else CoreAPIServer.omrDownload(f, name)

    response

  def directoryDownload(f: File, name: Option[String] = None, topDirectory: Boolean = true): IO[Response[IO]] =
    import org.openmole.tool.stream.*
    import org.openmole.tool.archive.*

    HTTP.sendFileStream(s"${name.getOrElse(f.getName)}.tgz"): out =>
      val tos = TarArchiveOutputStream(out.toGZ, blockSize = Some(64 * 1024))
      try tos.archive(f, includeTopDirectoryName = topDirectory)
      finally tos.close()

  def fileDownload(f: File, req: Request[IO], name: Option[String]): IO[Response[IO]] =
    HTTP.sendFile(req, f, name)

  def omrDownload(f: File, name: Option[String]): IO[Response[IO]] =
    import org.openmole.tool.stream.*
    import org.openmole.tool.archive.*
    import org.openmole.core.format.*
    val dataFiles = OMRFormat.dataFiles(f)
    HTTP.sendFileStream(s"${name.getOrElse(f.baseName)}.tgz"): out =>
      val tos = TarArchiveOutputStream(out.toGZ, blockSize = Some(64 * 1024))
      try
        tos.addFile(f, f.getName)
        dataFiles.foreach(n => tos.addFile(OMRFormat.dataFile(f, n), n))
      finally tos.close()

  def getSafePath(req: Request[IO]) =
    val fileType = req.params.get(org.openmole.gui.shared.api.fileTypeParam)
    val path = req.params.getOrElse(org.openmole.gui.shared.api.pathParam, throw new UserBadDataError(s"Parameter ${org.openmole.gui.shared.api.pathParam} is required"))

    fileType match
      case Some(fileType) => SafePath(path.split('/').toSeq, ServerFileSystemContext.fromTypeName(fileType).getOrElse(throw new InternalProcessingError(s"Unknown file type ${fileType}")))
      case None => SafePath(path.split('/').toSeq, ServerFileSystemContext.Project)



/** Defines a Play router (and reverse router) for the endpoints described
 * in the `CounterEndpoints` trait.
 */
class CoreAPIServer(apiImpl: ApiImpl, errorHandler: Throwable => IO[http4s.Response[IO]])
  extends APIServer
    with api.CoreAPI:

  override def handleServerError(request: http4s.Request[IO], throwable: Throwable): IO[http4s.Response[IO]] = errorHandler(throwable)

  val settingsRoute =
    omSettings.errorImplementedBy(_ => apiImpl.settings)

//  val isPasswordCorrectRoute =
//    isPasswordCorrect.safeImplementedBy(apiImpl.isPasswordCorrect _)

//  val resetPasswordRoute =
//    resetPassword.safeImplementedBy(_ => apiImpl.resetPassword())

  val listPluginsRoute =
    listPlugins.errorImplementedBy{ _ => apiImpl.listPlugins() }

  val guiPluginsRoute =
    guiPlugins.errorImplementedBy (_ => apiImpl.getGUIPlugins() )

  val listFilesRoute =
    listFiles.errorImplementedBy { (path, filter, withHidden) => apiImpl.listFiles(path, filter, testPlugin = true, withHidden = withHidden) }

  val sizeRoute =
    size.errorImplementedBy { path => apiImpl.size(path) }

  val saveFileRoute =
    saveFile.errorImplementedBy { case(path, fileContent, hash, overwrite) => apiImpl.saveFile(path, fileContent, hash, overwrite) }

  val copyFilesRoute =
    copyFiles.errorImplementedBy { case(sp, overwrite) => apiImpl.copyFiles(sp, overwrite) }

  val createFileRoute =
    createFile.errorImplementedBy { case(path, name, directory) => apiImpl.createFile(path, name, directory) }

  val extractArchiveRoute =
    extractArchive.errorImplementedBy { (sp, to) => apiImpl.extractArchive(sp, to) }

  val deleteFilesRoute =
    deleteFiles.errorImplementedBy { sp => apiImpl.deleteFiles(sp) }

  val existsRoute=
    exists.errorImplementedBy { sp => apiImpl.exists(sp) }

  val isTextRoute =
    isText.errorImplementedBy { sp => apiImpl.isTextFile(sp) }

  val listRecursiveRoute =
    listRecursive.errorImplementedBy { case(path, f, hidden) => apiImpl.recursiveListFiles(path, f, hidden) }

  val moveRoute =
    move.errorImplementedBy { p => apiImpl.move(p) }

  val mdToHtmlRoute =
    mdToHtml.errorImplementedBy { p => apiImpl.mdToHtml(p) }

  val sequenceRoute =
    sequence.errorImplementedBy { p => apiImpl.sequence(p) }

  val executionStateRoute =
    executionState.errorImplementedBy { i => apiImpl.executionData(i) }

  val executionOutputRoute =
    executionOutput.errorImplementedBy { (i, l) => apiImpl.executionOutput(i, l) }

  val cancelExecutionRoute =
    cancelExecution.errorImplementedBy { i => apiImpl.cancelExecution(i) }

  val removeExecutionRoute =
    removeExecution.errorImplementedBy { i => apiImpl.removeExecution(i) }

  val validateScriptRoute =
    validateScript.errorImplementedBy { s => apiImpl.validateScript(s) }

  val launchScriptRoute =
    launchScript.errorImplementedBy { (s, b) => apiImpl.launchScript(s, b) }

  val clearEnvironmentErrorsRoute =
    clearEnvironmentErrors.errorImplementedBy { case (eid, i) => apiImpl.clearEnvironmentErrors(eid, i) }

  val listEnvironmentErrorsRoute =
    listEnvironmentErrors.errorImplementedBy { case(eid, e, i) => apiImpl.listEnvironmentErrors(eid, e, i) }

//  val modelsRoute =
//    models.errorImplementedBy { p => apiImpl.models(p) }

//  val expandResourcesRoute =
//    expandResources.errorImplementedBy { r => apiImpl.expandResources(r) }

  val downloadHTTPRoute =
    downloadHTTP.errorImplementedBy { case(s, p, b, o) => apiImpl.downloadHTTP(s, p, b, o) }

  val temporaryDirectoryRoute =
    temporaryDirectory.errorImplementedBy { _ => apiImpl.temporaryDirectory() }

  val shutdownRoute =
    shutdown.errorImplementedBy { _ => apiImpl.shutdown() }

  val restartRoute =
    restart.errorImplementedBy { _ => apiImpl.restart() }

  val isAliveRoute =
    isAlive.implementedBy { _ => apiImpl.isAlive() }

  val jvmInfosRoute =
    jvmInfos.errorImplementedBy { _ => apiImpl.jvmInfos() }

  val marketIndexRoute =
    marketIndex.errorImplementedBy { _ => apiImpl.marketIndex() }

  val getMarketEntryRoute =
    getMarketEntry.errorImplementedBy { case (e, p) => apiImpl.getMarketEntry(e, p) }

  val omrMethodRoute =
    omrMethod.errorImplementedBy { p => apiImpl.omrMethodName(p) }

  val omrContentRoute =
    omrContent.errorImplementedBy { (p, d) => apiImpl.omrContent(p, d) }

  val omrFilesRoute =
    omrFiles.errorImplementedBy { p => apiImpl.omrFiles(p) }

  val omrDataIndexRoute =
    omrDataIndex.errorImplementedBy { p => apiImpl.omrDataIndex(p) }

  val addPluginRoute =
    addPlugin.errorImplementedBy { p => apiImpl.addPlugin(p) }

  val removePluginRoute =
    removePlugin.errorImplementedBy { p => apiImpl.removePlugin(p) }

  val listNotificationRoute =
    listNotification.errorImplementedBy { p => apiImpl.listNotification }

  val clearNotificationRoute =
    clearNotification.errorImplementedBy { s => apiImpl.clearNotification(s) }
  
  val endpointRoutes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      settingsRoute,
//      isPasswordCorrectRoute,
//      resetPasswordRoute,
      listPluginsRoute,
      guiPluginsRoute,
      listFilesRoute,
      sizeRoute,
      saveFileRoute,
      createFileRoute,
      extractArchiveRoute,
      deleteFilesRoute,
      existsRoute,
      isTextRoute,
      listRecursiveRoute,
      copyFilesRoute,
      moveRoute,
      mdToHtmlRoute,
      sequenceRoute,
      executionStateRoute,
      executionOutputRoute,
      cancelExecutionRoute,
      removeExecutionRoute,
      validateScriptRoute,
      launchScriptRoute,
      clearEnvironmentErrorsRoute,
      listEnvironmentErrorsRoute,
      downloadHTTPRoute,
      temporaryDirectoryRoute,
      marketIndexRoute,
      getMarketEntryRoute,
      omrMethodRoute,
      omrContentRoute,
      omrFilesRoute,
      omrDataIndexRoute,
      addPluginRoute,
      removePluginRoute,
      listNotificationRoute,
      clearNotificationRoute,
      restartRoute,
      shutdownRoute,
      jvmInfosRoute,
      isAliveRoute
    )
  ) //.map(_.putHeaders(Header("Access-Control-Allow-Origin", "*")))

  val routes: HttpRoutes[IO]  =
    HttpRoutes.of:
      case req @ POST -> Root / org.openmole.gui.shared.api.`uploadFilesRoute` =>
        import apiImpl.services.*
        import cats.effect.unsafe.implicits.global
        import org.http4s.multipart.*
        import org.openmole.gui.server.ext.utils
        import org.openmole.tool.stream.*

        def move(fileParts: Vector[Part[IO]], fileTypes: Seq[String]) =
          import org.openmole.gui.shared.data.ServerFileSystemContext
          def rootFile(fileType: String) =
            fileType match
              case ServerFileSystemContext.Project.typeName ⇒ utils.projectsDirectory
              case ServerFileSystemContext.Authentication.typeName ⇒ utils.authenticationKeysDirectory
              case ServerFileSystemContext.Absolute.typeName ⇒ new java.io.File("/")

          for
            (file, fileType) ← fileParts zip fileTypes
          do
            val destination = rootFile(fileType)
            apiImpl.unplug(HTTP.recieveDestination(file, destination))
            HTTP.recieveFile(file, destination)

        req.decode[Multipart[IO]]: parts =>
          def getFileParts = parts.parts.filter(_.filename.isDefined)
          move(getFileParts, HTTP.multipartStringContent(parts, "fileType").get.split(',').toSeq)
          Ok()

      case req @ GET -> Root / org.openmole.gui.shared.api.`downloadFileRoute` =>
        import apiImpl.services.*
        import org.openmole.tool.file.*

        import org.typelevel.ci.*

        val nameParam = req.params.get(org.openmole.gui.shared.api.Download.fileNameParam)

        val safePath = CoreAPIServer.getSafePath(req)

        val topDirectory = req.params.get(org.openmole.gui.shared.api.Download.topDirectoryParam).flatMap(_.toBooleanOption).getOrElse(true)
        val r = CoreAPIServer.download(req, safePath, nameParam, topDirectory = topDirectory)

        def addHashHeader(r: org.http4s.Response[IO], f: File) =
          val hash = req.params.get(org.openmole.gui.shared.api.Download.hashParam).flatMap(_.toBooleanOption).getOrElse(false)
          if hash
          then r.withHeaders(Header.Raw(CIString(org.openmole.gui.shared.api.hashHeader), apiImpl.services.fileService.hashNoCache(f).toString))
          else r

        r.map { r => addHashHeader(r, safePathToFile(safePath)) }

      case req @ GET -> p if p.renderString == s"/${org.openmole.gui.shared.api.convertOMRRoute}" =>
        import apiImpl.services.*
        val omrFile = safePathToFile(CoreAPIServer.getSafePath(req))
        val format = req.params.getOrElse(org.openmole.gui.shared.api.formatParam, throw new UserBadDataError(s"Parameter ${org.openmole.gui.shared.api.formatParam} is required"))
        val history = req.params.get(org.openmole.gui.shared.api.historyParam).map(_.toBoolean).getOrElse(false)

        HTTP.convertOMR(req, omrFile, GUIOMRContent.ExportFormat.fromString(format), history)



