package org.openmole.gui.server.core

import java.io.File
import java.text.SimpleDateFormat
import org.openmole.core.buildinfo
import org.openmole.core.event.*
import org.openmole.core.pluginmanager.*
import org.openmole.gui.shared.data.*

import java.io.*
import java.net.URL
import java.nio.file.*
import java.util.zip.GZIPInputStream
import org.openmole.core.compiler.*
import org.openmole.core.expansion.ScalaCompilation
import org.openmole.core.market.{MarketIndex, MarketIndexEntry}

import scala.util.{Failure, Success, Try}
import org.openmole.core.workflow.mole.{MoleExecution, MoleExecutionContext, MoleServices}
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm.*
import org.openmole.tool.tar.*
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.module
import org.openmole.core.market
import org.openmole.core.preference.{ConfigurationString, Preference, PreferenceLocation}
import org.openmole.core.project.*
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.dsl.*
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.gui.server.ext
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.server.core.GUIServer.{ApplicationControl, lockFile}
import org.openmole.gui.shared.data
import org.openmole.plugin.hook.omr.OMROutputFormat
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.outputredirection.OutputRedirection

import scala.collection.JavaConverters.*

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See theMarketIndexEntry
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class ApiImpl(val services: Services, applicationControl: Option[ApplicationControl]) {

  import ExecutionInfo._

  val outputSize = PreferenceLocation[Int]("gui", "outputsize", Some(10 * 1024 * 1024))

  val execution = new Execution

  //GENERAL
  def settings: OMSettings = {
    import services._

    OMSettings(
      utils.projectsDirectory.toSafePath,
      buildinfo.version.value,
      buildinfo.name,
      utils.formatDate(buildinfo.BuildInfo.buildTime),
      buildinfo.development
    )
  }

  def shutdown() = applicationControl.foreach(_.stop())
  def restart() = applicationControl.foreach(_.restart())

  def isAlive() = true

  def jvmInfos() = {
    val runtime = Runtime.getRuntime
    val totalMemory = runtime.totalMemory
    val allocatedMemory = totalMemory - runtime.freeMemory
    val javaVersion = System.getProperty("java.version")
    val jvmName = System.getProperty("java.vm.name")

    JVMInfos(
      javaVersion,
      jvmName,
      Runtime.getRuntime.availableProcessors,
      allocatedMemory,
      totalMemory
    )
  }


  //WORKSPACE
  def isPasswordCorrect(pass: String): Boolean = Preference.passwordIsCorrect(Cypher(pass), services.preference)

  //def passwordState = Utils.passwordState

  def resetPassword(): Unit = {
    import services._
    org.openmole.core.services.Services.resetPassword
  }

  // FILES
  def createFile(safePath: SafePath, name: String, directory: Boolean): Boolean =
    import services._
    if directory
    then new File(safePath.toFile, name).createNewFile
    else new File(safePath.toFile, name).mkdirs

  def deleteFiles(safePaths: Seq[SafePath]): Unit = {
    import services.*
    import org.openmole.tool.file.*
    
    val allPlugins = listPlugins()

    def unplug(f: File) =
      if utils.isPlugged(f, allPlugins.toSeq)(workspace) then removePlugin(utils.fileToSafePath(f))

    for
      safePath <- safePaths
      file = safePathToFile(safePath)
    do
     if file.isDirectory
     then file.applyRecursive(unplug)
     else unplug(file)

    utils.deleteFiles(safePaths)
  }

  private def getExtractedArchiveTo(from: File, to: File)(implicit context: ServerFileSystemContext): Seq[SafePath] = {
    import services._
    extractArchiveFromFiles(from, to)
    to.listFilesSafe.map(utils.fileToSafePath).toSeq
  }

  def unknownFormat(name: String) = Some(ErrorData("Unknown compression format for " + name))

  private def extractArchiveFromFiles(from: File, to: File) =
    Try {
      val ext = FileExtension(from.getName)
      ext match
        case FileExtension.Tar ⇒
          from.extract(to)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case FileExtension.TarGz ⇒
          from.extractUncompress(to, true)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case FileExtension.Zip ⇒ utils.unzip(from, to)
        case FileExtension.TarXz ⇒
          from.extractUncompressXZ(to, true)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case _ ⇒ throw new Throwable("Unknown compression format for " + from.getName)
    } match
      case Success(_) ⇒ None
      case Failure(t) ⇒ Some(ErrorData(t))



  def extract(safePath: SafePath) = {
    import services.*
    FileExtension(safePath.name) match {
      case FileExtension.TGZ | FileExtension.TAR | FileExtension.ZIP | FileExtension.TXZ ⇒
        val archiveFile = safePathToFile(safePath)
        val toFile: File = safePathToFile(safePath.parent)
        extractArchiveFromFiles(archiveFile, toFile)
      case _ ⇒ unknownFormat(safePath.name)
    }
  }

  def temporaryDirectory(): SafePath =
    import services.*
    val dir = services.tmpDirectory.newDir("openmoleGUI")
    dir.mkdirs()
    dir.toSafePath(using org.openmole.gui.shared.data.ServerFileSystemContext.Absolute)

  def exists(safePath: SafePath): Boolean = {
    import services._
    utils.exists(safePath)
  }

  def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean) =
    import services._
    utils.copyFiles(paths, overwrite)

  def listFiles(sp: SafePath, fileFilter: data.FileFilter = data.FileFilter.defaultFilter): ListFilesData = {
    import services.*
    utils.listFiles(sp, fileFilter, listPlugins())
  }

   def recursiveListFiles(sp: SafePath, findString: Option[String]): Seq[(SafePath, Boolean)] = {
    import services._
    utils.recursiveListFiles(sp, findString)
  }

  def isEmpty(sp: SafePath): Boolean = {
    import services._
    val f: File = safePathToFile(sp)
    f.isDirectoryEmpty
  }

  def move(from: SafePath, to: SafePath): Unit = {
    import services.*
    val fromFile = safePathToFile(from)
    val toFile = safePathToFile(to)

    fromFile.move(to.toFile)
    //utils.move(fromFile, toFile)
  }

  def duplicate(safePath: SafePath, newName: String): SafePath = {
    import services._
    utils.copyProjectFile(safePath, newName, followSymlinks = true)
  }

  def mdToHtml(safePath: SafePath): String = {
    import services._
    MarkDownProcessor(safePathToFile(safePath).content)
  }

  def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String) = {
    import services._

    val file = safePathToFile(path)

    file.withLock { _ ⇒
      def save() = {
        file.content = fileContent

        def newHash = services.fileService.hashNoCache(file).toString

        (true, newHash)
      }

      if (overwrite) save()
      else hash match {
        case Some(expectedHash: String) ⇒
          val hashOnDisk = services.fileService.hashNoCache(file).toString
          if (hashOnDisk == expectedHash) save() else (false, hashOnDisk)
        case _ ⇒ save()
      }
    }
  }

  def size(safePath: SafePath): Long = {
    import services._
    safePathToFile(safePath).length
  }

  def sequence(safePath: SafePath, separator: Char = ','): SequenceData = {
    import services._
    val content = safePath.toFile.content.split("\n")
    val regex = """\[[^\]]+\]|[+-]?[0-9][0-9]*\.?[0-9]*([Ee][+-]?[0-9]+)?|true|false""".r

    content.headOption.map { h ⇒
      SequenceData(
        h.split(',').toSeq,
        content.tail.map { row ⇒ regex.findAllIn(row).toSeq }.toSeq
      )
    }.getOrElse(SequenceData())
  }

  // EXECUTIONS
  def cancelExecution(id: ExecutionId): Unit = execution.cancel(id)

  def removeExecution(id: ExecutionId): Unit = execution.remove(id)

  def compileScript(script: SafePath) = {
    val (execId, outputStream) = compilationData(script)
    synchronousCompilation(execId, script, outputStream)
  }

  def runScript(script: SafePath, validateScript: Boolean) = {
    asynchronousCompilation(
      script,
      Some(execId ⇒ execution.compiled(execId)),
      Some(processRun(_, _, validateScript))
    )
  }

  private def compilationData(script: SafePath) = {
    import services._
    (ExecutionId(DataUtils.uuID) /*, safePathToFile(scriptData.scriptPath)*/ , StringPrintStream(Some(preference(outputSize))))
  }

  def synchronousCompilation(
    execId:       ExecutionId,
    scriptPath:   SafePath,
    outputStream: StringPrintStream,
    onCompiled:   Option[ExecutionId ⇒ Unit]                  = None,
    onEvaluated:  Option[(MoleExecution, ExecutionId) ⇒ Unit] = None): Option[ErrorData] = {

    def error(t: Throwable): ErrorData = {
      t match {
        case ce: Interpreter.CompilationError ⇒
          def toErrorWithLocation(em: Interpreter.ErrorMessage) =
            ErrorWithLocation(em.rawMessage, em.position.map {
              _.line
            }, em.position.map {
              _.start
            }, em.position.map {
              _.end
            })

          ErrorData(ce.errorMessages.map(toErrorWithLocation), t)
        case _ ⇒ ErrorData(t)
      }
    }

    def message(message: String) = MessageErrorData(message, None)

    val script: File = {
      import services._
      safePathToFile(scriptPath)
    }

    val executionOutputRedirection = OutputRedirection(outputStream)
    val executionTmpDirectory = services.tmpDirectory.newDir("execution")

    val runServices = {
      import services._
      Services.copy(services)(outputRedirection = executionOutputRedirection, newFile = TmpDirectory(executionTmpDirectory), fileServiceCache = FileServiceCache())
    }

    try {
      Project.compile(script.getParentFileSafe, script)(runServices) match {
        case ScriptFileDoesNotExists() ⇒ Some(message("Script file does not exist"))
        case ErrorInCode(e)            ⇒ Some(error(e))
        case ErrorInCompiler(e)        ⇒ Some(error(e))
        case compiled: Compiled ⇒
          import runServices._

          val executionServices =
            MoleServices.create(
              applicationExecutionDirectory = runServices.workspace.tmpDirectory,
              moleExecutionDirectory = Some(executionTmpDirectory),
              outputRedirection = Some(executionOutputRedirection),
              compilationContext = Some(compiled.compilationContext))

          onCompiled.foreach {
            _(execId)
          }
          catchAll(OutputManager.withStreamOutputs(outputStream, outputStream)(compiled.eval(Seq.empty)(runServices))) match {
            case Failure(e) ⇒ Some(error(e))
            case Success(dsl) ⇒
              Try(DSL.toPuzzle(dsl).toExecution()(executionServices)) match {
                case Success(ex) ⇒
                  onEvaluated.foreach {
                    _(ex, execId)
                  }
                  None
                case Failure(e) ⇒
                  MoleServices.clean(executionServices)
                  Some(error(e))
              }
          }
      }

    }
    catch {
      case t: Throwable ⇒ Some(error(t))
    }

  }

  def asynchronousCompilation(script: SafePath, onEvaluated: Option[ExecutionId ⇒ Unit] = None, onCompiled: Option[(MoleExecution, ExecutionId) ⇒ Unit] = None): Unit = {
    import services._
    val (execId, outputStream) = compilationData(script)

    val content = safePathToFile(script).content

    execution.addStaticInfo(execId, StaticExecutionInfo(script, content, System.currentTimeMillis()))
    execution.addOutputStreams(execId, outputStream)

    val compilationFuture: java.util.concurrent.Future[_] = threadProvider.submit(ThreadProvider.maxPriority) { () ⇒
      val errorData = synchronousCompilation(execId, script, outputStream, onEvaluated, onCompiled)
      errorData.foreach { ed ⇒ execution.addError(execId, Failed(Vector.empty, ed, Seq.empty)) }
    }

    execution.addCompilation(execId, compilationFuture)
  }

  def processRun(ex: MoleExecution, execId: ExecutionId, validateScript: Boolean) = {
    import services._
    val envIds = (ex.allEnvironments).map {
      env ⇒ EnvironmentId(DataUtils.uuID) → env
    }
    execution.addRunning(execId, envIds)
    envIds.foreach {
      case (envId, env) ⇒ env.listen(execution.environmentListener(envId))
    }

    catchAll(ex.start(validateScript)) match {
      case Failure(e) ⇒ execution.addError(execId, Failed(Vector.empty, ErrorData(e), Seq.empty))
      case Success(_) ⇒
        val inserted = execution.addMoleExecution(execId, ex)
        if (!inserted) ex.cancel
    }
  }

  def allStates(lines: Int) = execution.allStates(lines)

  def staticInfos() = execution.staticInfos()

  def clearEnvironmentErrors(environmentId: EnvironmentId): Unit = execution.deleteEnvironmentErrors(environmentId)

  def runningErrorEnvironmentData(environmentId: EnvironmentId, lines: Int): EnvironmentErrorData = atomic {
    implicit ctx ⇒
      val environmentErrors = execution.environmentErrors(environmentId)

      def groupedErrors = environmentErrors.groupBy {
        _.errorMessage
      }.toSeq.map {
        case (_, err) ⇒
          val dates = err.map {
            _.date
          }.sorted
          (err.head, dates.max, dates.size)
      }.takeRight(lines)

      EnvironmentErrorData(groupedErrors)
    //    EnvironmentErrorData(Seq(
    //      (EnvironmentError(environmentId, "YOur error man", Error("stansatienasitenasiruet a anuisetnasirte "), 2334454L, ErrorLevel()), 33345L, 2),
    //      (EnvironmentError(environmentId, "YOur error man 4", Error("stansatienasitenasiruet a anuaeiaiueaiueaieisetnasirte "), 2334454L, ErrorLevel()), 31345L, 1)
    //    ))
  }

  def marketIndex() = {
    import services._
    def mapToMd(marketIndex: MarketIndex) =
      marketIndex.copy(entries = marketIndex.entries.map {
        e ⇒
          e.copy(readme = e.readme.map {
            MarkDownProcessor(_)
          })
      })

    mapToMd(market.marketIndex)
  }

  def getMarketEntry(entry: MarketIndexEntry, path: SafePath) = {
    import services._
    market.downloadEntry(entry, safePathToFile(path))
    //autoAddPlugins(path)
  }

  private def toPluginList(currentPlugins: Seq[String]) =
    import services.*
    val currentPluginsSafePath = currentPlugins.map { s ⇒ SafePath(s.split("/")) }

    currentPluginsSafePath.map { csp ⇒
      val file = safePathToFile(csp)
      val date = ext.utils.formatDate(file.lastModified)
      Plugin(csp, date, file.exists && PluginManager.bundle(file).isDefined)
    }

  def activatePlugins =
    import services.*
    val plugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq()).map(s ⇒ safePathToFile(SafePath(s.split("/")))).filter(_.exists)
    PluginManager.tryLoad(plugins)

  private def isPlugged(safePath: SafePath) =
    import services._
    utils.isPlugged(safePathToFile(safePath), listPlugins())(workspace)

  private def updatePluggedList(set: Seq[String] ⇒ Seq[String]): Unit =
    import services._
    preference.updatePreference(GUIServer.plugins)(p => Some(set(p.getOrElse(Seq()))))

  def addPlugin(safePath: SafePath): Seq[ErrorData] = 
    import services._
    val errors = utils.addPlugin(safePath)
    if (errors.isEmpty) { updatePluggedList { pList ⇒ (pList :+ safePath.path.mkString("/")).distinct } }
    errors

  def listPlugins(): Seq[Plugin] =
    val currentPlugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq())
    toPluginList(currentPlugins)

  def removePlugin(safePath: SafePath) =
    import services.*
    updatePluggedList { _.filterNot(_ == safePath.path.mkString("/")) }
    utils.removePlugin(safePath)(workspace)

  def pluginRoutes =
    GUIPluginRegistry.all.flatMap(_.router).map(p => p(services))

  //GUI OM PLUGINS
  def getGUIPlugins(): PluginExtensionData =
    PluginExtensionData(
      GUIPluginRegistry.authentications,
      GUIPluginRegistry.wizards,
      GUIPluginRegistry.analysis)

  def isOSGI(safePath: SafePath): Boolean = {
    import services._

    PluginManager.isOSGI(safePathToFile(safePath))
  }

  // Analysis plugins
  def omrMethodName(result: SafePath): String =
    import services.*
    val omrFile = safePathToFile(result)
    OMROutputFormat.methodName(omrFile)

    //GUIPluginRegistry.analysis.find(_._1 == methodName).map(_._2)

  //MODEL WIZARDS

  //Extract models from an archive
  def models(archivePath: SafePath): Seq[SafePath] = {
    val toDir = archivePath.toNoExtention
    // extractTGZToAndDeleteArchive(archivePath, toDir)
    (for {
      tnd ← listFiles(toDir) if FileType.isSupportedLanguage(tnd.name)
    } yield tnd).map {
      nd ⇒ toDir ++ nd.name
    }
  }

  def expandResources(resources: Resources): Resources = {
    import services._

    val paths = resources.all.map(_.safePath).distinct.map {
      sp ⇒ Resource(sp, sp.toFile.length)
    }
    val implicitResource = resources.implicits.map {
      r ⇒ Resource(r.safePath, r.safePath.toFile.length)
    }

    Resources(
      paths,
      implicitResource,
      paths.size + implicitResource.size
    )
  }

  // FIXME use network service provider
  def downloadHTTP(url: String, path: SafePath, extract: Boolean): Option[ErrorData] =
    import services.*
    import org.openmole.tool.stream.*

    val result =
      Try {
        val checkedURL =
          java.net.URI.create(url).getScheme match {
            case null ⇒ "http://" + url
            case _    ⇒ url
          }

        gridscale.http.getResponse(checkedURL) {
          response ⇒
            def extractName = checkedURL.split("/").last

            val name =
              response.headers.flatMap {
                case ("Content-Disposition", value) ⇒
                  value.split(";").map(_.split("=")).find(_.head.trim == "filename").map {
                    filename ⇒
                      val name = filename.last.trim
                      if (name.startsWith("\"") && name.endsWith("\"")) name.drop(1).dropRight(1) else name
                  }
                case _ ⇒ None
              }.headOption.getOrElse(extractName)

            val is = response.inputStream

            if (extract) {
              val dest = safePathToFile(path)
              val tis = new TarInputStream(new GZIPInputStream(is))
              try tis.extract(dest)
              finally tis.close
            }
            else {
              val dest = safePathToFile(path / name)
              dest.withOutputStream(os ⇒ copy(is, os))
            }
        }
      }

    result match
      case Success(value) ⇒ None
      case Failure(e)     ⇒ Some(ErrorData(e))


}
