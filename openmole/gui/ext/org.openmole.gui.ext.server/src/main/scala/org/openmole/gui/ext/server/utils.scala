package org.openmole.gui.ext.server

import java.io.FileOutputStream
import java.nio.file.attribute.BasicFileAttributes
import java.util.logging.Level
import java.util.zip.ZipFile
import scala.collection.JavaConverters._
import org.openmole.core.fileservice._
import org.openmole.core.highlight.HighLight
import org.openmole.core.pluginmanager._
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.gui.ext.data
import org.openmole.gui.ext.data._
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

import scala.annotation.tailrec
import scala.io.{ BufferedSource, Codec }
import scala.util.{ Failure, Success, Try }

import collection.JavaConverters._

object utils {

  def pluginUpdoadDirectory(tmpDirectory: String)(implicit newFile: TmpDirectory) = newFile.directory / tmpDirectory

  def webUIDirectory()(implicit workspace: Workspace) = workspace.location /> "webui"

  def projectsDirectory()(implicit workspace: Workspace) = webUIDirectory /> "projects"

  def workspaceRoot()(implicit workspace: Workspace) = workspace.location

  def isPlugin(path: SafePath)(implicit workspace: Workspace): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    !PluginManager.listBundles(safePathToFile(path)).isEmpty
  }

  def allPluggableIn(path: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    path.toFile.listFilesSafe.filter { f ⇒ PluginManager.isBundle(f) }.map(_.toSafePath).toSeq
  }

  def treeNodeToSafePath(tnd: TreeNodeData, parent: SafePath): SafePath = parent ++ tnd.name

  implicit class SafePathDecorator(s: SafePath) {
    def toFile(implicit context: ServerFileSystemContext, workspace: Workspace) = safePathToFile(s)

    def copy(toPath: SafePath, withName: Option[String] = None)(implicit workspace: Workspace) = {
      import org.openmole.gui.ext.data.ServerFileSystemContext.project

      val from: File = s.toFile
      val to: File = toPath.toFile
      if (from.exists && to.exists) {
        FileDecorator(from).copy(toF = new File(to, withName.getOrElse(from.getName)), followSymlinks = true)
      }
    }
  }

  implicit class SafePathFileDecorator(f: File) {
    def toSafePath(implicit context: ServerFileSystemContext, workspace: Workspace) = fileToSafePath(f)
  }

  def fileToSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): SafePath = {
    context match {
      case _: ProjectFileSystem ⇒ SafePath(getPathArray(f, projectsDirectory))
      case _                    ⇒ SafePath(getPathArray(f, new File("")))
    }
  }

  def safePathToFile(s: SafePath)(implicit context: ServerFileSystemContext, workspace: Workspace): File = {
    context match {
      case _: ProjectFileSystem ⇒ getFile(webUIDirectory, s.path)
      case _                    ⇒ getFile(new File(""), s.path)
    }
  }

  def fileToTreeNodeData(f: File)(implicit context: ServerFileSystemContext = ProjectFileSystem()): Option[TreeNodeData] = {

    val time = if (f.exists) Some(
      java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
    )
    else None

    val dirData = if (f.isDirectory) Some(DirData(f.isDirectoryEmpty)) else None

    time.map(t ⇒ TreeNodeData(f.getName, dirData, f.length, t))
  }

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File])(implicit context: ServerFileSystemContext): Seq[TreeNodeData] = fs.flatMap {
    fileToTreeNodeData(_)
  }

  implicit def fileToOptionSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel = {
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorLevel()
    else DebugLevel()
  }

  def getPathArray(f: File, until: File): Seq[String] = {
    @tailrec
    def getParentsArray0(f: File, computedParents: Seq[String]): Seq[String] = {
      val parent = f.getParentFile
      if (parent != null) {
        val parentName = parent.getName
        if (parentName != "") {
          val computed = parentName +: computedParents
          if (parent == until) computed
          else getParentsArray0(parent, computed)
        }
        else computedParents
      }
      else computedParents
    }

    getParentsArray0(f, Seq()) :+ f.getName
  }

  def getFile(root: File, paths: Seq[String]): File = {
    @tailrec
    def getFile0(paths: Seq[String], accFile: File): File = {
      if (paths.isEmpty) accFile
      else getFile0(paths.tail, new File(accFile, paths.head))
    }

    getFile0(paths, root)
  }

  def listFiles(path: SafePath, fileFilter: data.FileFilter)(implicit context: ServerFileSystemContext, workspace: Workspace): ListFilesData = {

    val allFiles = safePathToFile(path).listFilesSafe.toSeq

    val filteredByName: Seq[TreeNodeData] = {
      if (fileFilter.nameFilter.isEmpty) allFiles
      else allFiles.filter { f ⇒ f.getName.contains(fileFilter.nameFilter) }
    }

    val sorted = filteredByName.sorted(fileFilter.fileSorting)
    val threshold = fileFilter.threshold.getOrElse(1000)
    val nbFiles = allFiles.size

    fileFilter.firstLast match {
      case First() ⇒ ListFilesData(sorted.take(threshold), nbFiles)
      case Last()  ⇒ ListFilesData(sorted.takeRight(threshold).reverse, nbFiles)
    }
  }

  def copyProjectFile(safePath: SafePath, newName: String, followSymlinks: Boolean = false)(implicit workspace: Workspace): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val toPath = safePath.copy(path = safePath.path.dropRight(1) :+ newName)
    if (toPath.toFile.isDirectory()) toPath.toFile.mkdir

    val from = safePath.toFile
    val replica = (safePath.parent ++ newName).toFile
    from.copy(replica, followSymlinks = followSymlinks)

    replica.toSafePath
  }

  def copyFile(from: File, to: File, create: Boolean = false): Unit = {
    if (create) to.mkdirs()
    if (from.exists && to.exists) from.copy(new File(to, from.getName))
  }

  private def buildClassTrees(classes: Seq[Seq[String]]): Seq[ClassTree] = {

    def build(classes: Seq[Seq[String]], classTrees: Seq[ClassTree]): Seq[ClassTree] = {
      val grouped = classes.groupBy {
        _.head
      }

      grouped.flatMap {
        case (k, v) ⇒
          val flatV = v.flatten
          if (flatV.size == 1) classTrees :+ ClassLeaf(flatV.head)
          else classTrees :+ ClassNode(
            k,
            build(v.map(_.tail), classTrees)
          )
      }.toSeq
    }

    build(classes, Seq())
  }

  def move(from: File, to: File): Unit =
    if (from.exists && to.exists) {
      from.move(new File(to, from.getName))
    }

  def exists(safePath: SafePath)(implicit workspace: Workspace) = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(safePath).exists
  }

  def existsExcept(in: SafePath, exceptItSelf: Boolean)(implicit workspace: Workspace): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val li = listFiles(in.parent, data.FileFilter.defaultFilter)
    val count = li.list.count(l ⇒ treeNodeToSafePath(l, in.parent).path == in.path)

    val bound = if (exceptItSelf) 1 else 0
    if (count > bound) true else false
  }

  def existsIn(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)
  }

  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath])(implicit workspace: Workspace): Unit = {
    val tmp: File = safePathToFile(tmpSafePath)(ServerFileSystemContext.absolute, workspace)

    filesToBeMovedTo.foreach { f ⇒
      val from = getFile(tmp, Seq(f.name))
      val toFile: File = safePathToFile(f.parent)(ServerFileSystemContext.project, workspace)
      copyFile(from, toFile)
    }

  }

  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath)(implicit workspace: Workspace): Unit = {

    val f: File = safePathToFile(tmpSafePath)(ServerFileSystemContext.absolute, workspace)
    val toFile: File = safePathToFile(to)(ServerFileSystemContext.project, workspace)

    val dirToCopy = {
      val level1 = f.listFiles.toSeq
      if (level1.size == 1) level1.head
      else f
    }

    toFile.mkdir
    dirToCopy.copy(toFile)

  }

  // Test if files exist in the 'to' directory, return the lists of already existing files or copy them otherwise
  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    val existing = existsIn(safePaths, to)

    if (existing.isEmpty) safePaths.foreach { sp ⇒ sp.copy(to) }
    existing
  }

  //copy safePaths files to 'to' folder in overwriting in they exist
  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace) = {
    import ServerFileSystemContext.project
    safePaths.foreach { sp ⇒
      sp.toFile.copy(new File(to.toFile, sp.name))
    }
  }

  def deleteFile(safePath: SafePath, context: ServerFileSystemContext)(implicit workspace: Workspace): Unit = {
    implicit val ctx = context
    safePathToFile(safePath).recursiveDelete
  }

  def deleteFiles(safePaths: Seq[SafePath], context: ServerFileSystemContext)(implicit workspace: Workspace): Unit = {
    safePaths.foreach { sp ⇒
      deleteFile(sp, context)
    }
  }

  def getUUID: String = java.util.UUID.randomUUID.toString

  val openmoleFileName = "main.js"
  val webpakedOpenmoleFileName = "openmole-webpacked.js"
  val depsFileName = "deps.js"
  val openmoleGrammarName = "openmole_grammar_template.js"
  val aceModuleSource = "ace-builds/src-noconflict"
  val openmoleGrammarMode = "mode-openmole.js"
  val githubTheme = "theme-github.js"
  val webpackConfigTemplateName = "template.webpack.config.js"
  val webpackJsonPackage = "package.json"

  def updateIfChanged(file: File)(update: File ⇒ Unit)(implicit fileService: FileService, newFile: TmpDirectory) = {
    import org.openmole.core.fileservice._

    def hash(f: File) = new File(f + "-hash")

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

  // Extract .zip archive
  def unzip(from: File, to: File) = {
    val basename = from.getName.substring(0, from.getName.lastIndexOf("."))
    to.getParentFile.mkdirs

    val zip = new ZipFile(from)
    zip.entries.asScala.foreach { entry ⇒
      val entryName = entry.getName
      if (entryName != s"$basename/") {
        val entryPath = {
          if (entryName.startsWith(basename))
            entryName.substring(basename.length)
          else
            entryName
        }

        val sub = new File(to, entryPath)
        if (entry.isDirectory) {
          if (!sub.exists) sub.mkdirs
        }
        else {
          // write file to dest
          val inputSrc = new BufferedSource(
            zip.getInputStream(entry)
          )(Codec.ISO8859)

          val ostream = new FileOutputStream(new File(to, entryPath))
          inputSrc foreach { c: Char ⇒ ostream.write(c) }
          inputSrc.close
          ostream.close

        }
      }
    }
  }

  //  def hash(safePath: SafePath)(implicit workspace: Workspace, context: ServerFileSystemContext) = {
  //    val file: File = safePathToFile(safePath)
  //
  //  }

  def catchAll[T](f: ⇒ T): Try[T] = {
    val res =
      try Success(f)
      catch {
        case t: Throwable ⇒ Failure(t)
      }
    res
  }

  def authenticationKeysFile(implicit workspace: Workspace) = workspace.persistentDir / "keys"

  def addPlugins(safePaths: Seq[SafePath])(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val files: Seq[File] = safePaths.map {
      safePathToFile
    }
    addFilePlugins(files)
  }

  def addFilePlugins(files: Seq[File])(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] = {
    val errors = org.openmole.core.module.addPluginsFiles(files, false, org.openmole.core.module.pluginDirectory)
    errors.map(e ⇒ ErrorData(e._2))
  }

  def removePlugin(plugin: Plugin)(implicit workspace: Workspace): Unit = synchronized {
    import org.openmole.core.module
    val file = module.pluginDirectory / plugin.name
    val allDependingFiles = PluginManager.allDepending(file, b ⇒ !b.isProvided)
    val bundle = PluginManager.bundle(file)
    bundle.foreach(PluginManager.remove)
    allDependingFiles.filter(f ⇒ !PluginManager.bundle(f).isDefined).foreach(_.recursiveDelete)
    file.recursiveDelete
  }
}
