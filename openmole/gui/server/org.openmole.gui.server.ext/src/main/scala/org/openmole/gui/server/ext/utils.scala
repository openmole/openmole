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
import org.openmole.core.services.Services
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.gui.shared.data
import org.openmole.gui.shared.data.*
import org.openmole.tool.file.*
import org.openmole.tool.logger.JavaLogger

import java.text.SimpleDateFormat
import scala.annotation.tailrec
import scala.io.{BufferedSource, Codec}
import scala.util.{Failure, Success, Try}
import collection.JavaConverters.*
import scala.collection.mutable.ListBuffer

object utils:

  def pluginUpdoadDirectory(tmpDirectory: String)(implicit newFile: TmpDirectory) = newFile.directory / tmpDirectory

  def webUIDirectory(implicit workspace: Workspace) = workspace.location /> "webui"

  def projectsDirectory(implicit workspace: Workspace) = webUIDirectory /> "projects"

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


  def fileToTreeNodeData(f: File, pluggedList: Seq[Plugin], testPlugin: Boolean = true)(using workspace: Workspace): Option[TreeNodeData] =
    def isPlugin(file: File): Boolean = testPlugin && PluginManager.isBundle(file)

    if f.exists()
    then
      val dirData = if (f.isDirectory) Some(TreeNodeData.Directory(f.isDirectoryEmpty)) else None
      val time = java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
      Some(TreeNodeData(f.getName, f.length, time, directory = dirData, pluginState = PluginState(isPlugin(f), isPlugged(f, pluggedList))))
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
            canonicalUntil.map {
              c =>
                if c.exists() && parent.exists()
                then java.nio.file.Files.isSameFile(parent.toPath, c.toPath)
                else parent.getPath == c.getPath
            }.getOrElse(false)

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
    def treeNodesData = safePathToFile(path).listFilesSafe.toSeq.filter(filterHidden).flatMap { f ⇒ fileToTreeNodeData(f, pluggedList, testPlugin = testPlugin) }
    val sorted = treeNodesData.sorted(FileSorting.toOrdering(fileFilter))

    val sortedSize = sorted.size

    fileFilter.size match
      case Some(s) => FileListData(sorted.take(s), s, sortedSize)
      case None => FileListData(sorted, sortedSize, sortedSize)

  def recursiveListFiles(path: SafePath, findString: Option[String])(implicit workspace: Workspace): Seq[(SafePath, Boolean)] =
    given ServerFileSystemContext = path.context

    val fPath = safePathToFile(path).getAbsolutePath
    val allFiles = safePathToFile(path).recursiveListFilesSafe((f: File) => fPath != f.getAbsolutePath && findString.map(s => f.getName.contains(s)).getOrElse(true))
    allFiles.map { f => (fileToSafePath(f), f.isDirectory) }

  def exists(safePath: SafePath)(implicit workspace: Workspace) =
    safePathToFile(safePath).exists

  def existsIn(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] =
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)

  def copyFiles(safePaths: Seq[(SafePath, SafePath)], overwrite: Boolean)(implicit workspace: Workspace): Seq[SafePath] =
    import org.openmole.core.omr.OMR
    val existing = ListBuffer[SafePath]()
    safePaths.foreach: (p, d) ⇒
      val destination = d.toFile
      def copy(): Unit =
        val fromFile = p.toFile
        if OMR.isOMR(fromFile)
        then OMR.copy(fromFile, destination)
        else fromFile.copy(destination)

      if destination.exists()
      then
        existing.append(d)
        if overwrite then copy()
      else copy()

    existing.toSeq


  def deleteFile(safePath: SafePath)(implicit workspace: Workspace): Unit =
    import org.openmole.core.omr.OMR
    val f = safePathToFile(safePath)
    if OMR.isOMR(f)
    then OMR.delete(f)
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

  def updateIfChanged(file: File, hashFile: Option[File] = None)(update: File ⇒ Unit)(implicit fileService: FileService, newFile: TmpDirectory) = {
    import org.openmole.core.fileservice._

    def hash(f: File) = hashFile.getOrElse(new File(f.toString + "-hash"))
    lockFile(file).withLock { _ ⇒
      val hashFile = hash(file)
      lazy val currentHash = fileService.hashNoCache(file).toString
      val upToDate =
        if (!file.exists || !hashFile.exists) false
        else
          Try(hashFile.content) match {
            case Success(v) ⇒ currentHash == v
            case Failure(_) ⇒ hashFile.delete; false
          }

      if (!upToDate) {
        update(file)
        hashFile.content = currentHash
      }
    }
  }


  def catchAll[T](f: ⇒ T): Try[T] = {
    val res =
      try Success(f)
      catch {
        case t: Throwable ⇒ Failure(t)
      }
    res
  }

  def authenticationKeysDirectory(implicit workspace: Workspace) = workspace.persistentDir / "keys"

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

  def formatDate(t: Long) = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(t)

