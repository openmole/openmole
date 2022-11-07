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
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.openmole.core.outputmanager.OutputManager
import org.openmole.gui.ext
import org.openmole.gui.ext.server.*
import org.openmole.gui.ext.{api, data}
import org.openmole.gui.server.core.{ApiImpl, GUIServerServices}

/** Defines a Play router (and reverse router) for the endpoints described
 * in the `CounterEndpoints` trait.
 */
class CoreAPIServer(apiImpl: ApiImpl)
  extends APIServer
    with api.CoreAPI {

  val settingsRoute =
    omSettings.implementedBy(_ => apiImpl.settings)

  val isPasswordCorrectRoute =
    isPasswordCorrect.implementedBy(apiImpl.isPasswordCorrect _)

//  val resetPasswordRoute =
//    resetPassword.implementedBy(_ => apiImpl.resetPassword())

  val listPluginsRoute =
    listPlugins.implementedBy(_ => apiImpl.listPlugins())

  val guiPluginsRoute =
    guiPlugins.implementedBy ( _ => apiImpl.getGUIPlugins())

  val listFilesRoute =
    listFiles.implementedBy { case(path, filter) => apiImpl.listFiles(path, filter) }

  val sizeRoute =
    size.implementedBy { path => apiImpl.size(path) }

  val saveFileRoute =
    saveFile.implementedBy { case(path, fileContent, hash, overwrite) => apiImpl.saveFile(path, fileContent, hash, overwrite) }

  val copyProjectFilesRoute =
    copyProjectFiles.implementedBy { case(sp, to, overwrite) => apiImpl.copyProjectFiles(sp, to, overwrite) }

  val renameKeyRoute =
    renameKey.implementedBy { case(name, newName) => apiImpl.renameKey(name, newName) }

  val createDirectoryRoute =
    createDirectory.implementedBy { case(path, name) => apiImpl.createDirectory(path, name) }

  val createFileRoute =
    createFile.implementedBy { case(path, name) => apiImpl.createFile(path, name) }

  val extractRoute =
    extract.implementedBy { sp => apiImpl.extractTGZ(sp) }

  val extractTestExistRoute =
    extractTestExist.implementedBy { case(a, b) => apiImpl.extractAndTestExistence(a, b) }

  val deleteFilesRoute =
    deleteFiles.implementedBy { sp => apiImpl.deleteFiles(sp) }

  val existsRoute=
    exists.implementedBy { sp => apiImpl.exists(sp) }

  val existsExceptRoute =
    existsExcept.implementedBy { case(sp, i) => apiImpl.existsExcept(sp, i) }

  val listRecursiveRoute =
    listRecursive.implementedBy { case(path, f) => apiImpl.recursiveListFiles(path, f) }

  val duplicateRoute =
    duplicate.implementedBy { case(p, n) => apiImpl.duplicate(p, n) }

  val moveRoute =
    move.implementedBy { case(f, t) => apiImpl.move(f, t) }

  val mdToHtmlRoute =
    mdToHtml.implementedBy { p => apiImpl.mdToHtml(p) }

  val renameRoute =
    rename.implementedBy { case(p, n) => apiImpl.renameFile(p, n) }

  val sequenceRoute =
    sequence.implementedBy { p => apiImpl.sequence(p) }

  val allStatesRoute =
    allStates.implementedBy { i => apiImpl.allStates(i) }

  val staticInfosRoute =
    staticInfos.implementedBy { _ => apiImpl.staticInfos() }

  val cancelExecutionRoute =
    cancelExecution.implementedBy { i => apiImpl.cancelExecution(i) }

  val removeExecutionRoute =
    removeExecution.implementedBy { i => apiImpl.removeExecution(i) }

  val compileScriptRoute =
    compileScript.implementedBy { s => apiImpl.compileScript(s) }

  val runScriptRoute =
    runScript.implementedBy { case(s, b) => apiImpl.runScript(s, b) }

  val clearEnvironmentErrorsRoute =
    clearEnvironmentErrors.implementedBy { i => apiImpl.clearEnvironmentErrors(i) }

  val runningErrorEnvironmentDataRoute =
    runningErrorEnvironmentData.implementedBy { case(e, i) => apiImpl.runningErrorEnvironmentData(e, i) }

  val modelsRoute =
    models.implementedBy { p => apiImpl.models(p) }

  val expandResourcesRoute =
    expandResources.implementedBy { r => apiImpl.expandResources(r) }

  val downloadHTTPRoute =
    downloadHTTP.implementedBy { case(s, p, b) => apiImpl.downloadHTTP(s, p, b) }

  val temporaryDirectoryRoute =
    temporaryDirectory.implementedBy { _ => apiImpl.temporaryDirectory() }

  val copyAllFromTmpRoute =
    copyAllFromTmp.implementedBy { case(f, t) => apiImpl.copyAllTmpTo(f, t) }

  val shutdownRoute =
    shutdown.implementedBy { _ => apiImpl.shutdown() }

  val restartRoute =
    restart.implementedBy { _ => apiImpl.restart() }

  val isAliveRoute =
    isAlive.implementedBy { _ => apiImpl.isAlive() }

  val jvmInfosRoute =
    jvmInfos.implementedBy { _ => apiImpl.jvmInfos() }

  val marketIndexRoute =
    marketIndex.implementedBy { _ => apiImpl.marketIndex() }

  val getMarketEntryRoute =
    getMarketEntry.implementedBy { case(e, p) => apiImpl.getMarketEntry(e, p) }

  val findAnalysisPluginRoute =
    findAnalysisPlugin.implementedBy { p => apiImpl.findAnalysisPlugin(p) }

  val addPluginRoute =
    addPlugin.implementedBy { p => apiImpl.addPlugin(p) }

  val removePluginRoute =
    removePlugin.implementedBy { p => apiImpl.removePlugin(p) }
  
  val endpointRoutes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      settingsRoute,
      isPasswordCorrectRoute,
//      resetPasswordRoute,
      listPluginsRoute,
      guiPluginsRoute,
      listFilesRoute,
      sizeRoute,
      saveFileRoute,
      renameKeyRoute,
      createDirectoryRoute,
      createFileRoute,
      extractRoute,
      deleteFilesRoute,
      existsRoute,
      existsExceptRoute,
      listRecursiveRoute,
      duplicateRoute,
      moveRoute,
      mdToHtmlRoute,
      renameRoute,
      sequenceRoute,
      allStatesRoute,
      staticInfosRoute,
      cancelExecutionRoute,
      removeExecutionRoute,
      compileScriptRoute,
      runScriptRoute,
      clearEnvironmentErrorsRoute,
      runningErrorEnvironmentDataRoute,
      modelsRoute,
      expandResourcesRoute,
      downloadHTTPRoute,
      temporaryDirectoryRoute,
      extractTestExistRoute,
      copyAllFromTmpRoute,
      marketIndexRoute,
      getMarketEntryRoute,
      findAnalysisPluginRoute,
      addPluginRoute,
      removePluginRoute
    )
  ) //.map(_.putHeaders(Header("Access-Control-Allow-Origin", "*")))

  val routes: HttpRoutes[IO]  =
    HttpRoutes.of {
      case req @ POST -> Root / org.openmole.gui.ext.data.routes.`uploadFilesRoute` =>
        import apiImpl.services.*
        import cats.effect.unsafe.implicits.global
        import org.http4s.multipart.*
        import org.openmole.gui.ext.server.utils
        import org.openmole.tool.stream.*

        def move(fileParts: Vector[Part[IO]], fileType: String, directoryName: Option[String]) = {

          def copyTo(rootFile: java.io.File) =
            for (file ← fileParts) {
              val path = new java.net.URI(file.name.get).getPath
              val destination = new java.io.File(rootFile, path)
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

          fileType match {
            case "project"        ⇒ copyTo(utils.webUIDirectory)
            case "authentication" ⇒ copyTo(utils.authenticationKeysFile)
            case "plugin"         ⇒ copyTo(utils.pluginUpdoadDirectory(directoryName.get))
            case "absolute"       ⇒ copyTo(new java.io.File(""))
          }
        }

        req.decode[Multipart[IO]] { parts =>
          def partContent(name: String) =
            parts.parts.find(_.name.exists(_ == name)).map(_.bodyText.compile.string.unsafeRunSync())

          def getFileParts = parts.parts.filter(_.filename.isDefined)

          move(getFileParts, partContent("fileType").get, partContent("directoryName"))
          Ok()
        }

      case req @ GET -> Root / org.openmole.gui.ext.data.routes.`downloadFileRoute` =>
        import apiImpl.services.*
        import org.openmole.tool.file.*

        val path = req.params("path")
        val hash = req.params.get("hash").flatMap(_.toBooleanOption).getOrElse(false)

        import org.typelevel.ci.*

        val f = new java.io.File(org.openmole.gui.ext.server.utils.webUIDirectory, path)

        if (!f.exists()) Status.NotFound.apply(s"The file $path does not exist.")
        else {
          if (f.isDirectory) {
            import org.openmole.tool.stream.*
            import org.openmole.tool.tar.*

            val st =
              fs2.io.readOutputStream[IO](64 * 1024) { out =>
                IO.blocking[Unit] {
                  val tos = new TarOutputStream(out.toGZ, 64 * 1024)
                  try tos.archive(f, includeTopDirectoryName = true)
                  finally tos.close
                }
              }

            Ok(st).map { r =>
              val r2 =
                r.withHeaders(
                  //org.http4s.headers.`Content-Length`(test.length()),
                  org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> s"${f.getName}.tgz"))
                )

              if (hash) r2.withHeaders(Header.Raw(CIString(org.openmole.gui.ext.data.routes.hashHeader), apiImpl.services.fileService.hashNoCache(f).toString))
              else r2
            }
          }
          else {
            f.withLock { _ ⇒
              StaticFile.fromFile(f, Some(req)).getOrElseF(Status.NotFound.apply()).map { r =>
                val r2 =
                  r.withHeaders(
                    org.http4s.headers.`Content-Length`(f.length()),
                    org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> s"${f.getName}"))
                  )

                if (hash) r2.withHeaders(Header.Raw(CIString(org.openmole.gui.ext.data.routes.hashHeader), apiImpl.services.fileService.hashNoCache(f).toString))
                else r2
              }
            }
          }
        }
    }


}
