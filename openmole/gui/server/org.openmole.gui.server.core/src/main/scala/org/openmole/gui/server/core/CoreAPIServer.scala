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
import cats.effect.unsafe.IORuntime
import org.http4s
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.format.OMRFormat
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workspace.Workspace
import org.openmole.gui.server.core.CoreAPIServer.download
import org.openmole.gui.server.ext
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.server.core.{ApiImpl, GUIServerServices}
import org.openmole.gui.shared.api
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*
import org.openmole.tool.archive.*

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.model.*
import sttp.tapir.server.http4s.*

object CoreAPIServer:
  def download(req: Request[IO], safePath: Seq[SafePath], name: Option[String] = None, topDirectory: Boolean = true)(using Workspace) =
    def downloadInArchive[T](name: String)(f: TarArchiveOutputStream => T): IO[Response[IO]] =
      import org.openmole.tool.stream.*

      HTTP.sendFileStream(name): out =>
        val tos = TarArchiveOutputStream(out.toGZ, blockSize = Some(64 * 1024))
        try f(tos)
        finally tos.close()

    def addOMR(tos: TarArchiveOutputStream, f: File) =
      import org.openmole.core.format.*
      val dataFiles = OMRFormat.dataFiles(f)
      tos.addFile(f, f.getName)
      dataFiles.foreach(n => tos.addFile(OMRFormat.dataFile(f, n), n))

    def addFile(tos: TarArchiveOutputStream, f: File) =
      if f.isDirectory
      then tos.archive(f, includeTopDirectoryName = topDirectory)
      else
        import org.openmole.core.format.*
        if !OMRFormat.isOMR(f)
        then tos.addFile(f, f.getName)
        else addOMR(tos, f)

    val notExists = safePath.filter(sp => !safePathToFile(sp).exists())

    if notExists.nonEmpty
    then Status.NotFound.apply(s"The file(s) ${notExists.map(_.path.mkString).mkString(" and ")} do(es) not exist.")
    else
      val files = safePath.map(safePathToFile)

      def archiveDirectoryName(f: File) = s"${name.getOrElse(f.getName)}.tar.gz"

      if files.size == 1 && !OMRFormat.isOMR(files.head)
      then
        val file = files.head
        if file.isDirectory
        then
          downloadInArchive(archiveDirectoryName(file)): os =>
            addFile(os, file)
        else CoreAPIServer.fileDownload(file, req, name)
      else
        val fileName =
          if files.size == 1
          then
            val f = files.head
            if f.isDirectory
            then archiveDirectoryName(f)
            else s"${name.getOrElse(f.baseName)}.tar.gz"
          else "files.tar.gz"

        downloadInArchive(fileName): os =>
          for f <- files
          do addFile(os, f)



  def fileDownload(f: File, req: Request[IO], name: Option[String]): IO[Response[IO]] =
    HTTP.sendFile(req, f, name)


  def getSafePath(req: Request[IO]): Seq[SafePath] =
    val fileType = req.multiParams.get(org.openmole.gui.shared.api.fileTypeParam)
    val path = req.multiParams.getOrElse(org.openmole.gui.shared.api.pathParam, throw UserBadDataError(s"Parameter ${org.openmole.gui.shared.api.pathParam} is required"))

    if path.isEmpty then throw UserBadDataError("Path param should be set to a value")

    fileType match
      case Some(fileType) =>
        (path lazyZip fileType).map: (p, ft) =>
          SafePath(p.split('/').toSeq, ServerFileSystemContext.fromTypeName(ft).getOrElse(throw new InternalProcessingError(s"Unknown file type ${fileType}")))
      case None => path.map(p => SafePath(p.split('/').toSeq, ServerFileSystemContext.Project))


  def apply(apiImpl: ApiImpl) = new CoreAPIServer(apiImpl)


class CoreAPIServer(apiImpl: ApiImpl):
  given IORuntime = apiImpl.services.threadProvider.ioRuntime

  val apiRoutes =
    import api.CoreAPI.*

    routesFromEndpoints(
      omSettings.implementedBy(_ => apiImpl.settings),
      shutdown.implementedBy(_ => apiImpl.shutdown()),
      isAlive.implementedBy(_ => apiImpl.isAlive()),
      jvmInfos.implementedBy(_ => apiImpl.jvmInfos()),
      listNotification.implementedBy(_ => apiImpl.listNotification),
      clearNotification.implementedBy(apiImpl.clearNotification),
      removeContainerCache.implementedBy(_ => apiImpl.removeContainerCache()),
      mdToHtml.implementedBy(apiImpl.mdToHtml),
      sequence.implementedBy(sp => apiImpl.sequence(sp)),
      size.implementedBy(apiImpl.size),
      saveFile.implementedBy(apiImpl.saveFile),
      createFile.implementedBy(apiImpl.createFile),
      extractArchive.implementedBy(apiImpl.extractArchive),
      isText.implementedBy(apiImpl.isTextFile),
      listRecursive.implementedBy(apiImpl.recursiveListFiles),
      listFiles.implementedBy((path, filter, withHidden) => apiImpl.listFiles(path, filter, testPlugin = true, withHidden = withHidden)),
      deleteFiles.implementedBy(apiImpl.deleteFiles),
      exists.implementedBy(apiImpl.exists),
      copyFiles.implementedBy(apiImpl.copyFiles),
      move.implementedBy(apiImpl.move),
      temporaryDirectory.implementedBy(_ => apiImpl.temporaryDirectory()),
      omrMethod.implementedBy(apiImpl.omrMethodName),
      omrContent.implementedBy(apiImpl.omrContent),
      omrFiles.implementedBy(apiImpl.omrFiles),
      omrDataIndex.implementedBy(apiImpl.omrDataIndex),
      cloneRepository.implementedBy(apiImpl.cloneRepository),
      commit.implementedBy(apiImpl.commit),
      revert.implementedBy(apiImpl.revert),
      add.implementedBy(apiImpl.add),
      pull.implementedBy(apiImpl.pull),
      branchList.implementedBy(apiImpl.branchList),
      checkout.implementedBy(apiImpl.checkout),
      stash.implementedBy(apiImpl.stash),
      stashPop.implementedBy(apiImpl.stashPop),
      push.implementedBy(apiImpl.push),
      listPlugins.implementedBy(_ => apiImpl.listPlugins()),
      guiPlugins.implementedBy(_ => apiImpl.getGUIPlugins()),
      executionState.implementedBy(apiImpl.executionData),
      executionOutput.implementedBy(apiImpl.executionOutput),
      cancelExecution.implementedBy(apiImpl.cancelExecution),
      removeExecution.implementedBy(apiImpl.removeExecution),
      validateScript.implementedBy(apiImpl.validateScript),
      launchScript.implementedBy(apiImpl.launchScript),
      clearEnvironmentErrors.implementedBy(apiImpl.clearEnvironmentErrors),
      listEnvironmentErrors.implementedBy(apiImpl.listEnvironmentErrors),
      downloadHTTP.implementedBy(apiImpl.downloadHTTP),
      marketIndex.implementedBy(_ => apiImpl.marketIndex()),
      getMarketEntry.implementedBy(apiImpl.getMarketEntry),
      addPlugin.implementedBy(apiImpl.addPlugin),
      removePlugin.implementedBy(apiImpl.removePlugin),
      gitAuthentications.implementedBy(_ => apiImpl.gitAuthentications),
      addGitAuthentication.implementedBy(apiImpl.addGitAuthentication),
      removeGitAuthentication.implementedBy(apiImpl.removeGitAuthentication),
      testGitAuthentication.implementedBy(apiImpl.testGitAuthentication)
    )

  val routes: HttpRoutes[IO]  =
    HttpRoutes.of:
      case req @ POST -> Root / org.openmole.gui.shared.api.`uploadFilesRoute` =>
        import apiImpl.services.*
        import org.http4s.multipart.*
        import org.openmole.gui.server.ext.utils
        import org.openmole.tool.stream.*
        import cats.effect.IO
        import cats.syntax.traverse.*
        import cats.instances.list.*


        def move(fileParts: Vector[Part[IO]], fileTypes: Seq[String]): IO[Unit] =
          import org.openmole.gui.shared.data.ServerFileSystemContext
          def rootFile(fileType: String) =
            fileType match
              case ServerFileSystemContext.Project.typeName => utils.projectsDirectory
              case ServerFileSystemContext.Authentication.typeName => utils.authenticationKeysDirectory
              case ServerFileSystemContext.Absolute.typeName => new java.io.File("/")

          (fileParts zip fileTypes).map: (file, fileType) =>
            val destination = rootFile(fileType)
            apiImpl.unplug(HTTP.recieveDestination(file, destination))
            HTTP.recieveFile(file, destination)
          .traverse(identity).map(_ => ())

        EntityDecoder.mixedMultipartResource[IO](maxParts = 1000).use: decoder =>
          req.decodeWith(decoder, strict = true): multipart =>
            def getFileParts = multipart.parts.filter(_.filename.isDefined)
            Ok(move(getFileParts, HTTP.multipartStringContent(multipart, "fileType").get.split(',').toSeq))

      case req @ GET -> Root / org.openmole.gui.shared.api.`downloadFileRoute` =>
        import apiImpl.services.*
        import org.openmole.tool.file.*

        import org.typelevel.ci.*

        val safePaths = CoreAPIServer.getSafePath(req)
        val nameParam = req.params.get(org.openmole.gui.shared.api.Download.fileNameParam)

        if safePaths.size == 1
        then
          val safePath = safePaths.head

          val topDirectory = req.params.get(org.openmole.gui.shared.api.Download.topDirectoryParam).flatMap(_.toBooleanOption).getOrElse(true)
          val r = CoreAPIServer.download(req, Seq(safePath), nameParam, topDirectory = topDirectory)

          def addHashHeader(r: org.http4s.Response[IO], f: File) =
            val hash = req.params.get(org.openmole.gui.shared.api.Download.hashParam).flatMap(_.toBooleanOption).getOrElse(false)
            if hash
            then r.withHeaders(org.http4s.Header.Raw(CIString(org.openmole.gui.shared.api.hashHeader), apiImpl.services.fileService.hashNoCache(f).toString))
            else r

          r.map { r => addHashHeader(r, safePathToFile(safePath)) }
        else CoreAPIServer.download(req, safePaths, nameParam)


      case req @ GET -> p if p.renderString == s"/${org.openmole.gui.shared.api.convertOMRRoute}" =>
        import apiImpl.services.*
        val omrFile = safePathToFile(CoreAPIServer.getSafePath(req).head)
        val format = req.params.getOrElse(org.openmole.gui.shared.api.formatParam, throw new UserBadDataError(s"Parameter ${org.openmole.gui.shared.api.formatParam} is required"))
        val history = req.params.get(org.openmole.gui.shared.api.historyParam).map(_.toBoolean).getOrElse(false)

        HTTP.convertOMR(req, omrFile, GUIOMRContent.ExportFormat.fromString(format), history)



