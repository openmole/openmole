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

    HTTP.sendFileStream(s"${name.getOrElse(f.getName)}.tar.gz"): out =>
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
    HTTP.sendFileStream(s"${name.getOrElse(f.baseName)}.tar.gz"): out =>
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

  val endpointRoutes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      omSettings.errorImplementedBy(_ => apiImpl.settings),
      listPlugins.errorImplementedBy(_ => apiImpl.listPlugins()),
      guiPlugins.errorImplementedBy(_ => apiImpl.getGUIPlugins()),
      listFiles.errorImplementedBy((path, filter, withHidden) => apiImpl.listFiles(path, filter, testPlugin = true, withHidden = withHidden)),
      size.errorImplementedBy(apiImpl.size),
      saveFile.errorImplementedBy(apiImpl.saveFile),
      createFile.errorImplementedBy(apiImpl.createFile),
      extractArchive.errorImplementedBy(apiImpl.extractArchive),
      deleteFiles.errorImplementedBy(apiImpl.deleteFiles),
      exists.errorImplementedBy(apiImpl.exists),
      isText.errorImplementedBy(apiImpl.isTextFile),
      listRecursive.errorImplementedBy(apiImpl.recursiveListFiles),
      copyFiles.errorImplementedBy(apiImpl.copyFiles),
      move.errorImplementedBy(apiImpl.move),
      mdToHtml.errorImplementedBy(apiImpl.mdToHtml),
      sequence.errorImplementedBy(sp => apiImpl.sequence(sp)),
      executionState.errorImplementedBy(apiImpl.executionData),
      executionOutput.errorImplementedBy(apiImpl.executionOutput),
      cancelExecution.errorImplementedBy(apiImpl.cancelExecution),
      removeExecution.errorImplementedBy(apiImpl.removeExecution),
      validateScript.errorImplementedBy(apiImpl.validateScript),
      launchScript.errorImplementedBy(apiImpl.launchScript),
      clearEnvironmentErrors.errorImplementedBy(apiImpl.clearEnvironmentErrors),
      listEnvironmentErrors.errorImplementedBy(apiImpl.listEnvironmentErrors),
      downloadHTTP.errorImplementedBy(apiImpl.downloadHTTP),
      temporaryDirectory.errorImplementedBy(_ => apiImpl.temporaryDirectory()),
      marketIndex.errorImplementedBy(_ => apiImpl.marketIndex()),
      getMarketEntry.errorImplementedBy(apiImpl.getMarketEntry),
      omrMethod.errorImplementedBy(apiImpl.omrMethodName),
      omrContent.errorImplementedBy(apiImpl.omrContent),
      omrFiles.errorImplementedBy(apiImpl.omrFiles),
      omrDataIndex.errorImplementedBy(apiImpl.omrDataIndex),
      addPlugin.errorImplementedBy(apiImpl.addPlugin),
      removePlugin.errorImplementedBy(apiImpl.removePlugin),
      listNotification.errorImplementedBy(_ => apiImpl.listNotification),
      clearNotification.errorImplementedBy(apiImpl.clearNotification),
      removeContainerCache.errorImplementedBy(_ => apiImpl.removeContainerCache()),
      shutdown.errorImplementedBy(_ => apiImpl.shutdown()),
      jvmInfos.errorImplementedBy(_ => apiImpl.jvmInfos()),
      isAlive.implementedBy(_ => apiImpl.isAlive()),
      cloneRepository.errorImplementedBy((r,d) => apiImpl.cloneRepository(r,d))
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



