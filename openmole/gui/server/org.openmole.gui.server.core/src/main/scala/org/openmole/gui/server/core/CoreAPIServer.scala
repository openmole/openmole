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
import org.openmole.gui.server.ext
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.server.core.{ApiImpl, GUIServerServices}
import org.openmole.gui.shared.api
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*

object CoreAPIServer:
  def getSafePath(req: Request[IO]) =
    val fileType = req.params.get(org.openmole.gui.shared.api.fileTypeParam)
    val path = req.params.getOrElse(org.openmole.gui.shared.api.pathParam, throw new UserBadDataError(s"Parameter ${org.openmole.gui.shared.api.pathParam} is required"))

    fileType match
      case Some(fileType) => SafePath(path.split('/').toSeq, ServerFileSystemContext.fromTypeName(fileType).getOrElse(throw new InternalProcessingError(s"Unknown file type ${fileType}")))
      case None => SafePath(path.split('/').toSeq)

  def setFileHeaders(f: File, r: Response[IO], name: Option[String] = None) =
    import org.typelevel.ci.*
    r.withHeaders(
      org.http4s.headers.`Content-Length`(f.length()),
      org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> name.getOrElse(f.getName)))
    )

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
    listFiles.errorImplementedBy { (path, filter, withHidden) => apiImpl.listFiles(path, filter, withHidden = withHidden) }

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

  val listRecursiveRoute =
    listRecursive.errorImplementedBy { case(path, f) => apiImpl.recursiveListFiles(path, f) }

  val moveRoute =
    move.errorImplementedBy { p => apiImpl.move(p) }

  val mdToHtmlRoute =
    mdToHtml.errorImplementedBy { p => apiImpl.mdToHtml(p) }

  val sequenceRoute =
    sequence.errorImplementedBy { p => apiImpl.sequence(p) }

  val executionStateRoute =
    executionState.errorImplementedBy { (l, i) => apiImpl.executionData(l, i) }

  val cancelExecutionRoute =
    cancelExecution.errorImplementedBy { i => apiImpl.cancelExecution(i) }

  val removeExecutionRoute =
    removeExecution.errorImplementedBy { i => apiImpl.removeExecution(i) }

  val compileScriptRoute =
    compileScript.errorImplementedBy { s => apiImpl.compileScript(s) }

  val launchScriptRoute =
    launchScript.errorImplementedBy { (s, b) => apiImpl.launchScript(s, b) }

  val clearEnvironmentErrorsRoute =
    clearEnvironmentErrors.errorImplementedBy { i => apiImpl.clearEnvironmentErrors(i) }

  val listEnvironmentErrorsRoute =
    listEnvironmentErrors.errorImplementedBy { case(e, i) => apiImpl.listEnvironmentErrors(e, i) }

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
    getMarketEntry.errorImplementedBy { case(e, p) => apiImpl.getMarketEntry(e, p) }

  val omrMethodRoute =
    omrMethod.errorImplementedBy { p => apiImpl.omrMethodName(p) }

  val omrContentRoute =
    omrContent.errorImplementedBy { p => apiImpl.omrContent(p) }

  val omrFilesRoute =
    omrFiles.errorImplementedBy { p => apiImpl.omrFiles(p) }

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
      listRecursiveRoute,
      copyFilesRoute,
      moveRoute,
      mdToHtmlRoute,
      sequenceRoute,
      executionStateRoute,
      cancelExecutionRoute,
      removeExecutionRoute,
      compileScriptRoute,
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

          for ((file, fileType) ← fileParts zip fileTypes) {
            val path = new java.net.URI(file.name.get).getPath
            val destination = new java.io.File(rootFile(fileType), path)
            destination.getParentFile.mkdirs()
            destination.setWritable(true)
            val stream = fs2.io.toInputStreamResource(file.body) //file._2.getInputStream
            stream.use { st =>
              IO {
                st.copy(destination)
                destination.setExecutable(true)
              }
            }.unsafeRunSync()
            //finally stream.close
          }

        req.decode[Multipart[IO]] { parts =>
          def partContent(name: String) =
            parts.parts.find(_.name.exists(_ == name)).map(_.bodyText.compile.string.unsafeRunSync())

          def getFileParts = parts.parts.filter(_.filename.isDefined)

          move(getFileParts, partContent("fileType").get.split(',').toSeq)
          Ok()
        }

      case req @ GET -> Root / org.openmole.gui.shared.api.`downloadFileRoute` =>
        import apiImpl.services.*
        import org.openmole.tool.file.*

        val hash = req.params.get(org.openmole.gui.shared.api.Download.hashParam).flatMap(_.toBooleanOption).getOrElse(false)
        val nameParam = req.params.get(org.openmole.gui.shared.api.Download.fileNameParam)

        import org.typelevel.ci.*

        val safePath = CoreAPIServer.getSafePath(req)
        val f = safePathToFile(safePath)

        def addHashHeader(r: org.http4s.Response[IO], f: File) =
          if hash
          then r.withHeaders(Header.Raw(CIString(org.openmole.gui.shared.api.hashHeader), apiImpl.services.fileService.hashNoCache(f).toString))
          else r

        def directoryDownload(f: File) =
          import org.openmole.tool.stream.*
          import org.openmole.tool.archive.*

          val topDirectory = req.params.get(org.openmole.gui.shared.api.Download.topDirectoryParam).flatMap(_.toBooleanOption).getOrElse(true)

          val st =
            fs2.io.readOutputStream[IO](64 * 1024): out =>
              IO.blocking[Unit]:
                val tos = new TarOutputStream(out.toGZ, 64 * 1024)
                try tos.archive(f, includeTopDirectoryName = topDirectory)
                finally tos.close()

          Ok(st).map: r =>
            val r2 =
              r.withHeaders(
                //org.http4s.headers.`Content-Length`(test.length()),
                org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> s"${nameParam.getOrElse(f.getName)}.tgz"))
              )

            addHashHeader(r2, f)

        def fileDownload(f: File) =
          f.withLock: _ ⇒
            StaticFile.fromPath(fs2.io.file.Path.fromNioPath(f.toPath), Some(req)).getOrElseF(Status.NotFound.apply()).map: r =>
              val r2 = CoreAPIServer.setFileHeaders(f, r)
              addHashHeader(r2, f)

        def omrDownload(f: File) =
          import org.openmole.tool.stream.*
          import org.openmole.tool.archive.*
          import org.openmole.core.omr.*

          val dataFiles = OMR.dataFiles(f)

          val st =
            fs2.io.readOutputStream[IO](64 * 1024): out =>
              IO.blocking[Unit]:
                val tos = new TarOutputStream(out.toGZ, 64 * 1024)
                try
                  tos.addFile(f, f.getName)
                  dataFiles.foreach((n, f) => tos.addFile(f, n))
                finally tos.close()

          Ok(st).map: r =>
            val r2 =
              r.withHeaders(
                //org.http4s.headers.`Content-Length`(test.length()),
                org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> s"${nameParam.getOrElse(f.baseName)}.tgz"))
              )
            addHashHeader(r2, f)

        if !f.exists()
        then Status.NotFound.apply(s"The file $path does not exist.")
        else
          if f.isDirectory
          then directoryDownload(f)
          else
            import org.openmole.core.omr.*
            if !OMR.isOMR(f)
            then fileDownload(f)
            else omrDownload(f)
      case req @ GET -> p if p.renderString == s"/${org.openmole.gui.shared.api.convertOMRToCSVRoute}" =>
        import apiImpl.services.*
        val omrFile = safePathToFile(CoreAPIServer.getSafePath(req))
        val fileBaseName = omrFile.baseName
        val csvFile = apiImpl.services.tmpDirectory.newFile(fileBaseName, ".csv")

        org.openmole.core.omr.OMR.writeCSV(omrFile, csvFile)
        def deleteCSVFile = IO[Unit] { csvFile.delete() }

        StaticFile.fromPath(fs2.io.file.Path.fromNioPath(csvFile.toPath), Some(req))
          .map{ req => req.withBodyStream(req.body.onFinalize(deleteCSVFile)) }
          .getOrElseF(Status.NotFound.apply())
          .map{ r => CoreAPIServer.setFileHeaders(csvFile, r, Some(s"$fileBaseName.csv")) }
      case req @ GET -> p if p.renderString == s"/${org.openmole.gui.shared.api.convertOMRToJSONRoute}" =>
        import apiImpl.services.*
        val omrFile = safePathToFile(CoreAPIServer.getSafePath(req))
        val fileBaseName = omrFile.baseName
        val jsonFile = apiImpl.services.tmpDirectory.newFile(fileBaseName, ".json")

        org.openmole.core.omr.OMR.writeJSON(omrFile, jsonFile)
        def deleteJSONFile = IO[Unit] { jsonFile.delete() }

        StaticFile.fromPath(fs2.io.file.Path.fromNioPath(jsonFile.toPath), Some(req))
          .map{ req => req.withBodyStream(req.body.onFinalize(deleteJSONFile)) }
          .getOrElseF(Status.NotFound.apply())
          .map{ r => CoreAPIServer.setFileHeaders(jsonFile, r, Some(s"$fileBaseName.json")) }



