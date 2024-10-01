package org.openmole.gui.server.ext

import java.io.FileOutputStream
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Level
import java.util.zip.ZipFile
import scala.collection.JavaConverters.*
import org.openmole.core.fileservice.*
import org.openmole.core.highlight.HighLight
import org.openmole.core.module
import org.openmole.core.pluginmanager.*
import org.openmole.core.serializer.SerializerService
import org.openmole.core.services.Services
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.gui.shared.data
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*
import org.openmole.tool.logger.JavaLogger
import org.openmole.gui.server.git.*
import org.eclipse.jgit.api.Git

import java.text.SimpleDateFormat
import scala.annotation.tailrec
import scala.io.{BufferedSource, Codec}
import scala.util.{Failure, Success, Try}
import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer


object utils:

  def pluginUpdoadDirectory(tmpDirectory: String)(implicit newFile: TmpDirectory) = newFile.directory / tmpDirectory

  def projectsDirectory(implicit workspace: Workspace) =
    val old = workspace.location / "webui" / "projects"
    val newProjects = workspace.userDir / "projects"
    if old.exists() then old.move(newProjects)
    newProjects.mkdirs()
    newProjects

  def workspaceRoot(implicit workspace: Workspace) = workspace.location

  def allPluggableIn(path: SafePath)(implicit workspace: Workspace): Seq[SafePath] =
    path.toFile.listFilesSafe.filter { f ⇒ PluginManager.isBundle(f) }.map(_.toSafePath).toSeq

  def treeNodeToSafePath(tnd: TreeNodeData, parent: SafePath): SafePath =
    parent ++ tnd.name

  implicit class SafePathDecorator(s: SafePath):
    def toFile(using Workspace) = safePathToFile(s)

  implicit class SafePathFileDecorator(f: File):
    def toSafePath(using context: ServerFileSystemContext = ServerFileSystemContext.Project, workspace: Workspace) = fileToSafePath(f)

  def fileToSafePath(f: File)(implicit context: ServerFileSystemContext = ServerFileSystemContext.Project, workspace: Workspace): SafePath =
    context match
      case ServerFileSystemContext.Project ⇒ SafePath(getPathArray(f, Some(projectsDirectory)), context)
      case ServerFileSystemContext.Absolute ⇒ SafePath(getPathArray(f, None), context)
      case ServerFileSystemContext.Authentication => SafePath(getPathArray(f, Some(authenticationKeysDirectory)), context)

  def safePathToFile(s: SafePath)(using workspace: Workspace): File =
    def getFile(root: Option[File], paths: Seq[String]): File =
      @tailrec
      def getFile0(paths: Seq[String], accFile: Option[File]): File =
        if (paths.isEmpty) accFile.getOrElse(new File(""))
        else
          accFile match
            case None => new File(paths.head)
            case Some(f) => getFile0(paths.tail, Some(new File(f, paths.head)))

      getFile0(paths, root)

    s.context match
      case ServerFileSystemContext.Project ⇒ getFile(Some(projectsDirectory), s.path.value)
      case ServerFileSystemContext.Absolute ⇒ getFile(Some(File("/")), s.path.value)
      case ServerFileSystemContext.Authentication => getFile(Some(authenticationKeysDirectory), s.path.value)

  def isPlugged(file: File, pluggedList: Seq[Plugin])(implicit workspace: Workspace): Boolean =
    val safePath = fileToSafePath(file)
    pluggedList.map {
      _.projectSafePath
    }.contains(safePath)


  def fileToTreeNodeData(f: File, pluggedList: Seq[Plugin], testPlugin: Boolean = true, gitStatus: Option[GitStatus] = None)(using workspace: Workspace): Option[TreeNodeData] =
    import org.openmole.core.format.OMRFormat
    def isPlugin(file: File): Boolean = testPlugin && PluginManager.isBundle(file)

    if f.exists()
    then
      val dirData = if (f.isDirectory) Some(TreeNodeData.Directory(f.isDirectoryEmpty)) else None
      val time = java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis

      def size =
        if OMRFormat.isOMR(f)
        then Try {
          OMRFormat.diskUsage(f)
        }.getOrElse(f.length())
        else f.length()

      Some(TreeNodeData(f.getName, size, time, directory = dirData, pluginState = PluginState(isPlugin(f), isPlugged(f, pluggedList)), gitStatus))
    else None

  //implicit def fileToOptionSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel =
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorStateLevel.Error
    else ErrorStateLevel.Debug

  def getPathArray(f: File, until: Option[File]): Seq[String] =
    val canonicalUntil = until.map(_.getCanonicalFile)

    @tailrec
    def getParentsArray0(f: File, computedParents: Seq[String]): Seq[String] =
      f.getParentFile match
        case null => computedParents
        case parent =>
          def sameFile =
            canonicalUntil.map: c =>
              if c.exists() && parent.exists()
              then java.nio.file.Files.isSameFile(parent.toPath, c.toPath)
              else parent.getPath == c.getPath
            .getOrElse(false)

          if sameFile
          then computedParents
          else
            parent.getName match
              case "" => computedParents
              case parentName =>
                val computed = parentName +: computedParents
                getParentsArray0(parent, computed)

    getParentsArray0(f, Seq()) :+ f.getName


  def listFiles(path: SafePath, fileFilter: FileSorting, pluggedList: Seq[Plugin], testPlugin: Boolean = true, withHidden: Boolean = true)(implicit workspace: Workspace): FileListData =
    given ServerFileSystemContext = path.context

    def filterHidden(f: File) = withHidden || !f.getName.startsWith(".")

    val currentDirGit = GitService.git(path.toFile, projectsDirectory)
    val modified = currentDirGit.map(GitService.getModified(_)).getOrElse(Seq())
    val conflicting = currentDirGit.map(GitService.getConflicting(_)).getOrElse(Seq())
    val untracked = currentDirGit.map(GitService.getUntracked(_)).getOrElse(Seq())

    val currentFile = safePathToFile(path)

    def treeNodesData =
      currentFile.listFilesSafe.toSeq.filter(filterHidden).flatMap: f ⇒
        val gitStatus = currentDirGit match
          case Some(g: Git) =>
            val relativeName = GitService.relativeName(f, g)
            if modified.contains(relativeName) then Some(GitStatus.Modified)
            else if untracked.contains(relativeName) then Some(GitStatus.Untracked)
            else if conflicting.contains(relativeName) then Some(GitStatus.Conflicting)
            else Some(GitStatus.Versioned)
          case None =>
            if f.isDirectory
            then
              GitService.git(f, currentFile) match
                case Some(g: Git) => Some(GitStatus.Root)
                case _ => None
            else None
        fileToTreeNodeData(f, pluggedList, testPlugin = testPlugin, gitStatus)

    val sorted = treeNodesData.sorted(FileSorting.toOrdering(fileFilter))
    val sortedSize = sorted.size

    val branchData =
      GitService.git(safePathToFile(path), projectsDirectory) map : git =>
        BranchData(GitService.branchList(git).map(_.split("/").last), git.getRepository.getBranch)

    fileFilter.size match
      case Some(s) => FileListData(sorted.take(s), s, sortedSize, branchData)
      case None => FileListData(sorted, sortedSize, sortedSize, branchData)

  def recursiveListFiles(path: SafePath, findString: Option[String], withHidden: Boolean = true)(implicit workspace: Workspace): Seq[(SafePath, Boolean)] =
    given ServerFileSystemContext = path.context

    def filterHidden(f: File) = withHidden || !f.getName.startsWith(".")

    val fPath = safePathToFile(path).getAbsolutePath
    val allFiles = safePathToFile(path).recursiveListFilesSafe((f: File) => filterHidden(f) && fPath != f.getAbsolutePath && findString.map(s => f.getName.contains(s)).getOrElse(true))
    allFiles.map { f => (fileToSafePath(f), f.isDirectory) }

  def exists(safePath: SafePath)(implicit workspace: Workspace) =
    safePathToFile(safePath).exists

  def existsIn(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] =
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)

  def copyFiles(safePaths: Seq[(SafePath, SafePath)], overwrite: Boolean)(implicit workspace: Workspace): Seq[SafePath] =
    import org.openmole.core.format.OMRFormat
    val existing = ListBuffer[SafePath]()
    safePaths.foreach: (p, d) ⇒
      val destination = d.toFile

      def copy(): Unit =
        val fromFile = p.toFile
        if OMRFormat.isOMR(fromFile)
        then OMRFormat.copy(fromFile, destination)
        else fromFile.copy(destination)

      if destination.exists()
      then
        existing.append(d)
        if overwrite then copy()
      else copy()

    existing.toSeq


  def deleteFile(safePath: SafePath)(implicit workspace: Workspace): Unit =
    import org.openmole.core.format.OMRFormat
    val f = safePathToFile(safePath)
    if OMRFormat.isOMR(f)
    then OMRFormat.delete(f)
    else f.recursiveDelete

  def deleteFiles(safePaths: Seq[SafePath])(implicit workspace: Workspace): Unit =
    safePaths.foreach: sp ⇒
      deleteFile(sp)

  val openmoleFileName = "main.js"
  val webpakedOpenmoleFileName = "openmole-webpacked.js"
  val depsFileName = "deps.js"
  val openmoleGrammarName = "openmole_grammar_template.js"
  val aceModuleSource = "ace-builds/src-noconflict"
  val openmoleGrammarMode = "mode-openmole.js"
  val githubTheme = "theme-github.js"
  val webpackConfigTemplateName = "template.webpack.config.js"
  val webpackJsonPackage = "package.json"
  val nodeModulesFileName = "node_modules.zip"

  def updateIfChanged(file: File, hashFile: Option[File] = None)(update: File ⇒ Unit)(implicit fileService: FileService, newFile: TmpDirectory) =
    import org.openmole.core.fileservice._

    def hash(f: File) = hashFile.getOrElse(new File(f.toString + "-hash"))

    lockFile(file).withLock: _ ⇒
      val hashFile = hash(file)
      lazy val currentHash = fileService.hashNoCache(file).toString
      val upToDate =
        if (!file.exists || !hashFile.exists) false
        else
          Try(hashFile.content) match
            case Success(v) ⇒ currentHash == v
            case Failure(_) ⇒
              hashFile.delete
              false

      if !upToDate
      then
        update(file)
        hashFile.content = currentHash


  def catchAll[T](f: ⇒ T): Try[T] =
    val res =
      try Success(f)
      catch
        case t: Throwable ⇒ Failure(t)
    res

  def authenticationKeysDirectory(implicit workspace: Workspace) = workspace.userDir / "keys"

  def addPlugin(safePath: SafePath)(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] =
    val file = safePathToFile(safePath)
    val errors = org.openmole.core.pluginmanager.PluginManager.tryLoad(Seq(file))
    errors.map(e ⇒ ErrorData(e._2)).toSeq


  def removePlugin(safePath: SafePath)(implicit workspace: Workspace): Unit = synchronized {
    import org.openmole.core.module
    val file: File = safePathToFile(safePath)
    val bundle = PluginManager.bundle(file)
    bundle.foreach(PluginManager.remove)
  }

  object HTTP:

    import cats.effect.*
    import org.http4s
    import org.http4s.*
    import org.http4s.dsl.io.*
    import org.http4s.headers.*
    import org.http4s.implicits.*
    import org.http4s.multipart.{Multipart, Part}

    def multipartContent(multipart: Multipart[IO], name: String, shouldBeFile: Boolean = false) =
      multipart.parts.find(_.name.contains(name)).filter(f => !shouldBeFile || f.filename.isDefined)

    def listMultipartContent(multipart: Multipart[IO], name: String, shouldBeFile: Boolean = false) =
      multipart.parts.filter(_.name.contains(name)).filter(f => !shouldBeFile || f.filename.isDefined)

    def multipartStringContent(multipart: Multipart[IO], name: String)(using cats.effect.unsafe.IORuntime) =
      multipartContent(multipart, name).map(_.bodyText.compile.string.unsafeRunSync())

    def recieveDestination(part: Part[IO], directory: File) =
      val path = new java.net.URI(part.name.get).getPath
      new java.io.File(directory, path)

    def recieveFile(part: Part[IO], directory: File): IO[Unit] =
      val destination = recieveDestination(part, directory)

      def copyToFile(stream: fs2.Stream[IO, Byte], path: fs2.io.file.Path) =
        stream
          .through(fs2.io.file.Files[IO].writeAll(path))
          .compile
          .drain

      destination.getParentFile.mkdirs()
      copyToFile(part.body, fs2.io.file.Path.fromNioPath(destination.toPath)).map: _ =>
        destination.setWritable(true)

    def sendFileStream(fileName: String)(stream: java.io.OutputStream => Unit) =
      import org.typelevel.ci.*
      val st =
        fs2.io.readOutputStream[IO](64 * 1024): out =>
          IO.blocking[Unit]:
            stream(out)

      Ok(st).map: r =>
        r.withHeaders(
          //org.http4s.headers.`Content-Length`(test.length()),
          org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> s"$fileName"))
        )

    def sendFile(req: Request[IO], f: File, name: Option[String] = None) =
      StaticFile.fromPath(fs2.io.file.Path.fromNioPath(f.toPath), Some(req)).getOrElseF(Status.NotFound.apply(s"${f.getName} not found")).map: r =>
        setFileHeaders(f, r, name = name)

    def setFileHeaders(f: File, r: Response[IO], name: Option[String] = None) =
      import org.typelevel.ci.*
      r.withHeaders(
        org.http4s.headers.`Content-Length`(f.length()),
        org.http4s.headers.`Content-Disposition`("attachment", Map(ci"filename" -> name.getOrElse(f.getName)))
      )

    def convertOMRHistory(omrFile: File, format: GUIOMRContent.ExportFormat)(using tmpDirectory: TmpDirectory, serializerService: SerializerService) =
      import org.openmole.tool.archive.*
      import org.openmole.tool.stream.*

      val history = org.openmole.core.format.OMRFormat.dataFiles(omrFile)

      HTTP.sendFileStream(s"${omrFile.baseName}.tar.gz"): out =>
        val tos = TarArchiveOutputStream(out.toGZ, blockSize = Some(64 * 1024))
        try
          history.zipWithIndex.foreach: (h, i) =>
            val f = tmpDirectory.newFile(s"history$i")
            try
              format match
                case GUIOMRContent.ExportFormat.JSON =>
                  org.openmole.core.format.OMRFormat.writeJSON(omrFile, f, dataFile = Some(h))
                  tos.addFile(f, s"$i.json")
                case GUIOMRContent.ExportFormat.CSV =>
                  org.openmole.core.format.OMRFormat.writeCSV(omrFile, f, dataFile = Some(h))
                  tos.addFile(f, s"$i.csv")
            finally f.delete()
        finally tos.close()

    def convertOMR(req: Request[IO], omrFile: File, format: GUIOMRContent.ExportFormat, history: Boolean)(using tmpDirectory: TmpDirectory, serializerService: SerializerService) =
      if !history
      then
        val fileBaseName = omrFile.baseName
        val (exportFile, exportName) =
          format match
            case GUIOMRContent.ExportFormat.JSON =>
              val f = tmpDirectory.newFile(fileBaseName, ".json")
              org.openmole.core.format.OMRFormat.writeJSON(omrFile, f)
              (f, s"$fileBaseName.json")
            case GUIOMRContent.ExportFormat.CSV =>
              val f = tmpDirectory.newFile(fileBaseName, ".csv")
              org.openmole.core.format.OMRFormat.writeCSV(omrFile, f)
              (f, s"$fileBaseName.csv")

        def deleteExportFile = IO[Unit]:
          exportFile.delete()

        StaticFile.fromPath(fs2.io.file.Path.fromNioPath(exportFile.toPath), Some(req))
          .map { req => req.withBodyStream(req.body.onFinalize(deleteExportFile)) }
          .getOrElseF(Status.NotFound.apply())
          .map { r => HTTP.setFileHeaders(exportFile, r, Some(exportName)) }
      else convertOMRHistory(omrFile, format)

    def stackError(t: Throwable) =
      import org.openmole.core.tools.io.Prettifier.*
      import io.circe.*
      import io.circe.syntax.*
      import io.circe.generic.auto.*
      import org.openmole.gui.shared.data.*
      import org.http4s.headers.`Content-Type`
      InternalServerError {
        Left(ErrorData(t)).asJson.noSpaces
      }.map(_.withContentType(`Content-Type`(MediaType.application.json)))
