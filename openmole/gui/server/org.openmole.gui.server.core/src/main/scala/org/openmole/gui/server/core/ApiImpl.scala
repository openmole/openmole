package org.openmole.gui.server.core

import org.eclipse.jgit.api.Git

import java.io.File
import java.text.SimpleDateFormat
import org.openmole.core.buildinfo
import org.openmole.core.event.*
import org.openmole.core.pluginmanager.*
import org.openmole.gui.shared.data.{ServerFileSystemContext, *}

import java.io.*
import java.net.URL
import java.nio.file.*
import java.util.zip.GZIPInputStream
import org.openmole.core.compiler.*
import org.openmole.core.context.Variable
import org.openmole.core.argument.ScalaCompilation
import org.openmole.core.market.{MarketIndex, MarketIndexEntry}

import scala.util.{Failure, Success, Try}
import org.openmole.core.workflow.mole.{MoleExecution, MoleExecutionContext, MoleServices}
import org.openmole.tool.stream.StringPrintStream

import scala.concurrent.stm.*
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.module
import org.openmole.core.market
import org.openmole.core.preference.{ConfigurationString, Preference, PreferenceLocation}
import org.openmole.core.project.*
import org.openmole.core.services.Services
import org.openmole.core.threadprovider.{ThreadProvider, toExecutionContext}
import org.openmole.core.dsl.*
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.format.*
import org.openmole.core.format.OMROutputFormat
import org.openmole.core.workflow.mole.MoleExecution.MoleExecutionFailed
import org.openmole.gui.server.ext
import org.openmole.gui.server.ext.*
import org.openmole.gui.server.ext.utils.*
import org.openmole.gui.server.core.GUIServer.{ApplicationControl, lockFile}
import org.openmole.gui.server.git.GitService
import org.openmole.gui.shared.data
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.outputredirection.OutputRedirection

import scala.jdk.FutureConverters.*

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

  import ExecutionState._

  val outputSize = PreferenceLocation[Int]("gui", "outputsize", Some(10 * 1024 * 1024))

  val serverState = new ServerState

  //GENERAL
  def settings: OMSettings =
    import services._

    OMSettings(
      utils.projectsDirectory.toSafePath,
      buildinfo.version.value,
      buildinfo.name,
      buildinfo.BuildInfo.buildTime,
      buildinfo.development
    )

  def shutdown() = applicationControl.foreach(_.stop())

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
    val target = new File(safePath.toFile, name)
    if directory
    then target.mkdirs
    else target.createNewFile


  def deleteFiles(safePaths: Seq[SafePath]): Unit =
    import services.*
    import org.openmole.tool.file.*

    for
      safePath <- safePaths
      file = safePathToFile(safePath)
    do unplug(file)

    utils.deleteFiles(safePaths)

  //  private def getExtractedArchiveTo(from: File, to: File)(implicit context: ServerFileSystemContext): Seq[SafePath] = {
  //    import services._
  //    extractArchiveFromFiles(from, to)
  //    to.listFilesSafe.map(utils.fileToSafePath).toSeq
  //  }
  //

  private def archiveType(f: File) =
    import org.openmole.tool.archive.*
    val name = f.getName
    name match
      case n if n.endsWith(".tar") ⇒ Some(ArchiveType.Tar)
      case n if n.endsWith(".tgz") | n.endsWith(".tar.gz") ⇒ Some(ArchiveType.TarGZ)
      case n if n.endsWith(".zip") ⇒ Some(ArchiveType.Zip)
      case n if n.endsWith(".tar.xz") | n.endsWith("txz") ⇒ Some(ArchiveType.TarXZ)
      case _ ⇒ None

  private def extractArchiveFromFiles(from: File, to: File) =
    import org.openmole.tool.archive.*
    Try {
      val name = from.getName
      archiveType(name) match
        case Some(ArchiveType.Tar) ⇒
          from.extract(to, archive = ArchiveType.Tar)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case Some(ArchiveType.TarGZ) ⇒
          from.extract(to, true, archive = ArchiveType.TarGZ)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case Some(ArchiveType.Zip) ⇒
          import org.openmole.tool.archive
          from.extract(to, true, archive = ArchiveType.Zip)
        case Some(ArchiveType.TarXZ) ⇒
          from.extract(to, true, archive = ArchiveType.TarXZ)
          to.applyRecursive((f: File) ⇒ f.setWritable(true))
        case None ⇒ throw new Throwable("Unknown compression format for file " + from)
    } match
      case Success(_) ⇒ None
      case Failure(t) ⇒ Some(ErrorData(t))

  def extractArchive(safePath: SafePath, to: SafePath) =
    import services.*
    def archiveFile = safePathToFile(safePath)

    def toFile = safePathToFile(to)

    extractArchiveFromFiles(archiveFile, toFile)

  def temporaryDirectory(): SafePath =
    import services.*
    val dir = services.tmpDirectory.newDir("openmoleGUI", create = true)
    dir.toSafePath(using org.openmole.gui.shared.data.ServerFileSystemContext.Absolute)

  def exists(safePath: SafePath): Boolean =
    import services._
    utils.exists(safePath)

  def isTextFile(safePath: SafePath): Boolean =
    import services._
    safePathToFile(safePath).isTextFile()

  def copyFiles(paths: Seq[(SafePath, SafePath)], overwrite: Boolean) =
    import services._
    utils.copyFiles(paths, overwrite)

  def listFiles(sp: SafePath, fileFilter: data.FileSorting = data.FileSorting(), testPlugin: Boolean = true, withHidden: Boolean = true): FileListData =
    import services.*
    utils.listFiles(sp, fileFilter, listPlugins(), testPlugin = testPlugin, withHidden = withHidden)

  def recursiveListFiles(sp: SafePath, findString: Option[String], withHidden: Boolean): Seq[(SafePath, Boolean)] =
    import services._
    utils.recursiveListFiles(sp, findString, withHidden)

  def isEmpty(sp: SafePath): Boolean =
    import services._
    val f: File = safePathToFile(sp)
    f.isDirectoryEmpty

  def move(moves: Seq[(SafePath, SafePath)]): Unit =
    moves.foreach: (from, to) =>
      import services.*
      val fromFile = safePathToFile(from)
      val toFile = safePathToFile(to)
      toFile.getParentFile.mkdirs()
      if OMRFormat.isOMR(fromFile)
      then OMRFormat.move(fromFile, toFile)
      else
        unplug(fromFile)
        fromFile.move(toFile)

  def mdToHtml(safePath: SafePath): String =
    import services._
    MarkDownProcessor(safePathToFile(safePath).content)

  def saveFile(path: SafePath, fileContent: String, hash: Option[String], overwrite: Boolean): (Boolean, String) =
    import services._

    val file = safePathToFile(path)
    if !file.exists() then file.content = ""

    file.withLock { _ ⇒
      def save() =
        val tmpFile = Files.createTempFile(file.getParentFile, s".${file.getName}", ".tmp").toFile
        tmpFile.mode = file
        try
          tmpFile.content = fileContent
          tmpFile move file
        finally tmpFile.delete()

        def newHash = services.fileService.hashNoCache(file).toString

        (true, newHash)

      if (overwrite) save()
      else hash match {
        case Some(expectedHash: String) ⇒
          val hashOnDisk = services.fileService.hashNoCache(file).toString
          if (hashOnDisk == expectedHash) save() else (false, hashOnDisk)
        case _ ⇒ save()
      }
    }

  def size(safePath: SafePath): Long =
    import services._
    safePathToFile(safePath).length

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
  def cancelExecution(id: ExecutionId): Unit = serverState.cancel(id)

  def removeExecution(id: ExecutionId): Unit = serverState.remove(id)

  def validateScript(script: SafePath) =
    import services.*
    val outputStream = StringPrintStream(Some(preference(outputSize)))
    synchronousCompilation(script, outputStream) match
      case e: ErrorData => Some(e)
      case e: MoleExecution =>
        try
          e.validate
          None
        catch
          case e: Throwable => Some(ErrorData(e))
        finally
          MoleExecution.clean(e)

  def synchronousCompilation(
                              scriptPath: SafePath,
                              outputStream: StringPrintStream): ErrorData | MoleExecution =

    def scriptError(t: Throwable): ErrorData =
      t match
        case ce: Interpreter.CompilationError ⇒
          def toErrorWithLocation(em: Interpreter.ErrorMessage) =
            ScriptError(
              em.rawMessage,
              position = em.position.map { p => ScriptError.Position(p.line, p.point, p.start, p.end) }
            )

          ErrorData(ce.errorMessages.map(toErrorWithLocation), t)
        case _ ⇒ ErrorData(t)

    def message(message: String) = MessageErrorData(message, None)

    val script: File =
      import services._
      safePathToFile(scriptPath)

    val executionOutputRedirection = OutputRedirection(outputStream)
    val executionTmpDirectory = services.tmpDirectory.newDir("execution")

    val runServices =
      import services._
      Services.copy(services)(outputRedirection = executionOutputRedirection, newFile = TmpDirectory(executionTmpDirectory), fileServiceCache = FileServiceCache())

    try
      Project.compile(script.getParentFileSafe, script)(runServices) match
        case ScriptFileDoesNotExists() ⇒ ErrorData("Script file does not exist")
        case ErrorInCode(e) ⇒ scriptError(e)
        case ErrorInCompiler(e) ⇒ scriptError(e)
        case compiled: Compiled ⇒
          if Thread.interrupted() then throw new InterruptedIOException()
          import runServices._

          val executionServices =
            MoleServices.create(
              applicationExecutionDirectory = runServices.workspace.tmpDirectory,
              moleExecutionDirectory = Some(executionTmpDirectory),
              outputRedirection = Some(executionOutputRedirection),
              compilationContext = Some(compiled.compilationContext))

          catchAll(OutputManager.withStreamOutputs(outputStream, outputStream)(compiled.eval(Seq.empty)(runServices))) match
            case Failure(e) ⇒ scriptError(e)
            case Success(dsl) ⇒
              Try(MoleExecution(dsl)(executionServices)) match
                case Success(ex) ⇒ ex
                case Failure(e) ⇒
                  MoleServices.clean(executionServices)
                  scriptError(e)

    catch
      case t: Throwable ⇒ scriptError(t)


  def launchScript(script: SafePath, validateScript: Boolean) =
    import services.*

    val execId = ExecutionId()
    val outputStream = StringPrintStream(Some(preference(outputSize)))

    val content = safePathToFile(script).content

    import scala.util.control.Breaks
    def processRun(execId: ExecutionId, ex: MoleExecution, validateScript: Boolean): Unit = Breaks.breakable:
      import services._

      val envIds =
        try ex.allEnvironments.map { env ⇒ EnvironmentId(randomId) → env }
        catch
          case e: Throwable =>
            serverState.modifyState(execId, Failed(Vector.empty, ErrorData(e), Seq.empty))
            Breaks.break()

      serverState.setEnvironments(execId, envIds)
      ex.listen(serverState.moleExecutionListener(execId, script))
      envIds.foreach { (envId, env) ⇒ env.listen(serverState.environmentListener(execId, envId)) }

      catchAll(ex.start(validateScript)) match
        case Failure(e) ⇒
          serverState.modifyState(execId, Failed(Vector.empty, ErrorData(e), Seq.empty))
        case Success(_) ⇒
          val inserted = serverState.modifyState(execId, ex)
          if !inserted || Thread.currentThread().isInterrupted then ex.cancel

    def compileAndRun =
      synchronousCompilation(script, outputStream) match
        case e: MoleExecution =>
          if Thread.interrupted() then throw new InterruptedIOException()
          processRun(execId, e, validateScript)
        case ed: ErrorData => serverState.modifyState(execId, Failed(Vector.empty, ed, Seq.empty))


    val future =
      threadProvider.newSingleThreadExecutor.submit:
        () => compileAndRun

    serverState.addExecutionInfo(execId, ServerState.ExecutionInfo(script, content, System.currentTimeMillis(), outputStream, future, Map.empty))

    execId


  def executionData(ids: Seq[ExecutionId]): Seq[ExecutionData] = serverState.executionData(ids)

  def executionOutput(id: ExecutionId, lines: Int) = serverState.executionOutput(id, lines)

  def executionIds = serverState.executionIds.toSeq

  //def staticInfos() = execution.staticInfos()

  def clearEnvironmentErrors(executionId: ExecutionId, environmentId: EnvironmentId): Unit = serverState.clearEnvironmentErrors(executionId: ExecutionId, environmentId)

  def listEnvironmentErrors(executionId: ExecutionId, environmentId: EnvironmentId, lines: Int): Seq[EnvironmentError] =
    atomic {
      implicit ctx ⇒
        val environmentErrors = serverState.environmentErrors(executionId, environmentId)

        //      def groupedErrors =
        //          environmentErrors.groupBy { _.errorMessage }.toSeq.map {
        //            case (_, err) ⇒
        //              val dates = err.map { _.date }.sorted
        //              EnvironmentErrorGroup(err.head, dates.max, dates.size)
        //          }.takeRight(lines)

        val (errors, warning) = environmentErrors.partition(_.level == ErrorStateLevel.Error)
        val res = (errors.sortBy(_.date).reverse ++ warning.sortBy(_.date).reverse).take(lines)
        res
      //    EnvironmentErrorData(Seq(
      //      (EnvironmentError(environmentId, "YOur error man", Error("stansatienasitenasiruet a anuisetnasirte "), 2334454L, ErrorLevel()), 33345L, 2),
      //      (EnvironmentError(environmentId, "YOur error man 4", Error("stansatienasitenasiruet a anuaeiaiueaiueaieisetnasirte "), 2334454L, ErrorLevel()), 31345L, 1)
      //    ))
    }

  def marketIndex() =
    import services._
    def mapToMd(marketIndex: MarketIndex) =
      marketIndex.copy(entries = marketIndex.entries.map {
        e ⇒
          e.copy(readme = e.readme.map {
            MarkDownProcessor(_)
          })
      })

    mapToMd(market.marketIndex)

  def getMarketEntry(entry: MarketIndexEntry, path: SafePath) = {
    import services._
    market.downloadEntry(entry, safePathToFile(path))
    //autoAddPlugins(path)
  }

  private def toPluginList(currentPlugins: Seq[String]) =
    import services.*
    val currentPluginsSafePath = currentPlugins.map { s ⇒ SafePath(s.split("/"), ServerFileSystemContext.Project) }

    currentPluginsSafePath.flatMap { csp ⇒
      val file = safePathToFile(csp)
      val date = file.lastModified
      if file.exists
      then Some(Plugin(csp, date, PluginManager.bundle(file).isDefined))
      else None
    }

  def activatePlugins =
    import services.*
    val plugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq()).map(s ⇒ safePathToFile(SafePath(s.split("/"), ServerFileSystemContext.Project))).filter(_.exists)
    PluginManager.tryLoad(plugins)

  //  private def isPlugged(safePath: SafePath) =
  //    import services._
  //    utils.isPlugged(safePathToFile(safePath), listPlugins())(workspace)

  private def updatePluggedList(set: Seq[String] ⇒ Seq[String]): Unit =
    import services._
    preference.updatePreference(GUIServer.plugins)(p => Some(set(p.getOrElse(Seq()))))

  def addPlugin(safePath: SafePath): Seq[ErrorData] =
    import services._
    val errors = utils.addPlugin(safePath)
    if (errors.isEmpty) {
      updatePluggedList { pList ⇒ (pList :+ safePath.path.value.mkString("/")).distinct }
    }
    errors

  def listPlugins(): Seq[Plugin] =
    val currentPlugins = services.preference.preferenceOption(GUIServer.plugins).getOrElse(Seq())
    toPluginList(currentPlugins)

  def removePlugin(safePath: SafePath) =
    import services.*
    updatePluggedList {
      _.filterNot(_ == safePath.path.value.mkString("/"))
    }
    utils.removePlugin(safePath)(workspace)

  def unplug(file: File) =
    import services.*
    import org.openmole.tool.file.*

    val allPlugins = listPlugins()

    def unplugFile(f: File) =
      if utils.isPlugged(f, allPlugins)(workspace)
      then removePlugin(utils.fileToSafePath(f))

    if file.exists()
    then
      if file.isDirectory
      then file.applyRecursive(unplugFile)
      else unplugFile(file)

  def pluginRoutes =
    GUIPluginRegistry.all.flatMap(_.router).map(p => p(services))

  //GUI OM PLUGINS
  def getGUIPlugins(): PluginExtensionData =
    PluginExtensionData(
      GUIPluginRegistry.authentications,
      GUIPluginRegistry.wizards,
      GUIPluginRegistry.analysis)

  def isOSGI(safePath: SafePath): Boolean =
    import services._
    PluginManager.isOSGI(safePathToFile(safePath))

  // Analysis plugins
  def omrMethodName(result: SafePath): Option[String] =
    import services.*
    val omrFile = safePathToFile(result)
    OMRFormat.methodName(omrFile)

  def omrFiles(omr: SafePath): Option[SafePath] =
    import services.*
    val omrFile = safePathToFile(omr)
    OMRFormat.resultFileDirectory(omrFile).map: rf =>
      fileToSafePath(rf)

  def omrContent(result: SafePath, dataFile: Option[String]): GUIOMRContent =
    import services.*
    import GUIVariable.ValueType
    import GUIVariable.ValueType.*
    val omrFile = safePathToFile(result)

    def content =
      OMRFormat.variables(omrFile, dataFile = dataFile).map: (s, v) =>
        GUIOMRSectionContent(s.name, v.map(toGUIVariable))

    val omrContent = OMRFormat.omrContent(omrFile)

    def script =
      def convertImport(i: OMRContent.Import) = GUIOMRImport(`import` = i.`import`, content = i.content)

      omrContent.script.map(s => GUIOMRScript(content = s.content, `import` = s.`import`.getOrElse(Seq()).map(convertImport)))

    def index =
      val names = omrContent.`data-content`.section.flatMap(_.indexes)
      val size = omrContent.`data-file`.size
      if names.flatten.isEmpty || size <= 1
      then None
      else Some(GUIOMRContent.Index(size, names))

    def raw =
      tmpDirectory.withTmpFile("result", ".csv"): csvFile =>
        OMRFormat.writeCSV(omrFile, csvFile, dataFile)
        csvFile.content

    GUIOMRContent(
      section = content,
      openMoleVersion = omrContent.`openmole-version`,
      executionId = omrContent.`execution-id`,
      script = script,
      timeStart = omrContent.`time-start`,
      timeSave = omrContent.`time-save`,
      index = index,
      raw = raw
    )


  def omrDataIndex(result: SafePath): Seq[GUIOMRDataIndex] =
    import services.*
    import GUIVariable.ValueType
    import GUIVariable.ValueType.*
    val omrFile = safePathToFile(result)

    def indexes =
      OMRFormat.indexes(omrFile).map: id =>
        GUIOMRDataIndex(id.sectionIndex, id.variableName, id.values.flatMap(toValueTypeFromAny), id.fileIndex.toSeq)

    indexes

  def toValueTypeFromAny(x: Any): Option[GUIVariable.ValueType] =
    import services.*
    import GUIVariable.ValueType
    import GUIVariable.ValueType.*
    import ServerFileSystemContext.Project

    def res =
      util.Try:
        x match
          case x: Int => ValueInt(x)
          case x: Long => ValueLong(x)
          case x: Double => ValueDouble(x)
          case x: String => ValueString(x)
          case x: Boolean => ValueBoolean(x)
          case x: File => ValueFile(fileToSafePath(x))

          case x: Array[Int] => ValueArrayInt(x)
          case x: Array[Long] => ValueArrayLong(x)
          case x: Array[Double] => ValueArrayDouble(x)
          case x: Array[String] => ValueArrayString(x)
          case x: Array[Boolean] => ValueArrayBoolean(x)
          case x: Array[File] => ValueArrayFile(x.map(fileToSafePath))

          case x: Array[Array[Int]] => ValueArrayArrayInt(x)
          case x: Array[Array[Long]] => ValueArrayArrayLong(x)
          case x: Array[Array[Double]] => ValueArrayArrayDouble(x)
          case x: Array[Array[String]] => ValueArrayArrayString(x)
          case x: Array[Array[Boolean]] => ValueArrayArrayBoolean(x)
          case x: Array[Array[File]] => ValueArrayArrayFile(x.map(_.map(fileToSafePath)))

    res match
      case util.Failure(exception: MatchError) => None
      case util.Success(value) => Some(value)
      case util.Failure(e: Throwable) => throw e


  def toGUIVariable(v: Variable[_]) =
    GUIVariable(v.name, toValueTypeFromAny(v.value), v.prototype.`type`.toString)

  def expandResources(resources: Resources): Resources =
    import services._

    val paths =
      resources.all.map(_.safePath).distinct.map: sp ⇒
        Resource(sp, sp.toFile.length)

    val implicitResource =
      resources.implicits.map: r ⇒
        Resource(r.safePath, r.safePath.toFile.length)

    Resources(
      paths,
      implicitResource,
      paths.size + implicitResource.size
    )

  def listNotification = serverState.listNotification()

  def clearNotification(ids: Seq[Long]) = serverState.clearNotification(ids)

  def removeContainerCache(): Unit =
    org.openmole.plugin.task.container.ContainerTask.repositoryDirectory(services.workspace).recursiveDelete

  def downloadHTTP(url: String, path: SafePath, extract: Boolean, overwrite: Boolean): Unit =
    import services.*
    import org.openmole.tool.stream.*

    val checkedURL =
      java.net.URI.create(url).getScheme match {
        case null ⇒ "http://" + url
        case _ ⇒ url
      }

    NetworkService.withResponse(checkedURL) {
      response ⇒
        def extractName = checkedURL.split("/").last

        val name =
          response.getAllHeaders.map(h => h.getName -> h.getValue).flatMap {
            case ("Content-Disposition", value) ⇒
              value.split(";").map(_.split("=")).find(_.head.trim == "filename").map {
                filename ⇒
                  val name = filename.last.trim
                  if (name.startsWith("\"") && name.endsWith("\"")) name.drop(1).dropRight(1) else name
              }
            case _ ⇒ None
          }.headOption.getOrElse(extractName)

        val is = response.getEntity.getContent

        if
          extract && (archiveType(safePathToFile(path)) contains org.openmole.tool.archive.ArchiveType.TarGZ)
        then
          import org.openmole.tool.archive.*
          val dest = safePathToFile(path)
          val tis = TarArchiveInputStream(new GZIPInputStream(is))
          try tis.extract(dest, overwrite = overwrite)
          finally tis.close()
        else
          val dest = safePathToFile(path / name)
          if !dest.exists() || overwrite
          then dest.withOutputStream(os ⇒ copy(is, os))
          else throw new IOException(s"Destination file $dest already exists and overwrite is not set")
    }

  def cloneRepository(remoteURL: String, destination: SafePath): Unit =
    import services.workspace
    GitService.clone(remoteURL, destination.toFile)

  def commit(paths: Seq[SafePath], message: String): Unit =
    import services.workspace
    paths.headOption.foreach: hf =>
      GitService.withGit(safePathToFile(hf), projectsDirectory): git=>
        GitService.commit(paths.map(_.toFile), message)(git)
      
  def revert(paths: Seq[SafePath]): Unit =
    import services.workspace
    paths.headOption.foreach: hf =>
      GitService.withGit(safePathToFile(hf), projectsDirectory): git=>
          GitService.revert(paths.map(_.toFile))(git)
          
  def add(paths: Seq[SafePath]): Unit =
    import services.workspace
    paths.headOption.foreach: hf =>
      GitService.withGit(safePathToFile(hf), projectsDirectory): git=>
          GitService.add(paths.map(_.toFile))(git)
          
  def pull(from: SafePath): Unit =
    import services.workspace
    GitService.withGit(safePathToFile(from), projectsDirectory): git=>
          GitService.pull(git)
          
  def branchList(from: SafePath): Option[BranchData] =
    import services.workspace
    GitService.git(safePathToFile(from), projectsDirectory) map: git=>
        BranchData(GitService.branchList(git).map(_.split("/").last), git.getRepository.getBranch)
        
  def checkout(from: SafePath, branchName: String): Unit =
    import services.workspace
    GitService.withGit(safePathToFile(from), projectsDirectory): git=>
      GitService.checkout(branchName)(git)
        
  def stash(from: SafePath): Unit = 
    import services.workspace
    GitService.withGit(safePathToFile(from), projectsDirectory): git=>
      GitService.stash(git)
      
  def stashPop(from: SafePath): Unit = 
    import services.workspace
    GitService.withGit(safePathToFile(from), projectsDirectory): git=>
      GitService.stashPop(git)
}
