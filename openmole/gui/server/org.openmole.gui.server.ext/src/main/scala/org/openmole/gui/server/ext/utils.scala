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

object utils {

  def pluginUpdoadDirectory(tmpDirectory: String)(implicit newFile: TmpDirectory) = newFile.directory / tmpDirectory

  def webUIDirectory(implicit workspace: Workspace) = workspace.location /> "webui"

  def projectsDirectory(implicit workspace: Workspace) = webUIDirectory /> "projects"

  def workspaceRoot(implicit workspace: Workspace) = workspace.location

  def isPlugin(path: SafePath)(implicit workspace: Workspace): Boolean = {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project
    // !PluginManager.listBundles(safePathToFile(path)).isEmpty
    PluginManager.isBundle(safePathToFile(path))
  }

  def isPlugged(file: File, pluggedList: Seq[Plugin])(implicit workspace: Workspace): Boolean = {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project
    val safePath = fileToSafePath(file)
    pluggedList.map {
      _.projectSafePath
    }.contains(safePath)
  }

  def allPluggableIn(path: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project
    path.toFile.listFilesSafe.filter { f ⇒ PluginManager.isBundle(f) }.map(_.toSafePath).toSeq
  }

  def treeNodeToSafePath(tnd: TreeNodeData, parent: SafePath): SafePath = parent ++ tnd.name

  implicit class SafePathDecorator(s: SafePath) {
    def toFile(implicit workspace: Workspace) =
      given ServerFileSystemContext = s.context
      safePathToFile(s)

    def copy(toPath: SafePath, withName: Option[String] = None)(implicit workspace: Workspace) = {
      import org.openmole.gui.shared.data.ServerFileSystemContext.project

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

  def fileToSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): SafePath =
    context match
      case ServerFileSystemContext.Project ⇒ SafePath(getPathArray(f, Some(projectsDirectory)))
      case ServerFileSystemContext.Absolute ⇒ SafePath(getPathArray(f, None))
      case ServerFileSystemContext.Authentication => SafePath(getPathArray(f, Some(authenticationKeysDirectory)))

  def safePathToFile(s: SafePath)(implicit workspace: Workspace): File =
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
      case ServerFileSystemContext.Project ⇒ getFile(Some(projectsDirectory), s.path)
      case ServerFileSystemContext.Absolute ⇒ getFile(None, s.path)
      case ServerFileSystemContext.Authentication => getFile(Some(authenticationKeysDirectory), s.path)

  def fileToTreeNodeData(f: File, pluggedList: Seq[Plugin])(implicit context: ServerFileSystemContext = ServerFileSystemContext.Project, workspace: Workspace): Option[TreeNodeData] = {

    val time = if (f.exists) Some(
      java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
    )
    else None

    val dirData = if (f.isDirectory) Some(DirData(f.isDirectoryEmpty)) else None

    time.map(t ⇒ TreeNodeData(f.getName, dirData, f.length, t, PluginState(isPlugin(fileToSafePath(f)), isPlugged(f, pluggedList))))
  }

  def seqfileToSeqTreeNodeData(fs: Seq[File], pluggedList: Seq[Plugin])(implicit context: ServerFileSystemContext, workspace: Workspace): Seq[TreeNodeData] = fs.flatMap { f ⇒
    fileToTreeNodeData(f, pluggedList)
  }

  implicit def fileToOptionSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel = {
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorLevel()
    else DebugLevel()
  }

  def getPathArray(f: File, until: Option[File]): Seq[String] =
    val canonicalUntil = until.map(_.getCanonicalFile)

    @tailrec
    def getParentsArray0(f: File, computedParents: Seq[String]): Seq[String] =
      f.getParentFile match
        case null => computedParents
        case parent =>
          parent.getName match
            case "" => computedParents
            case parentName =>
              val computed = parentName +: computedParents
              if canonicalUntil.map(f == _).getOrElse(false)
              then computed
              else getParentsArray0(parent, computed)

    getParentsArray0(f.getCanonicalFile, Seq()) :+ f.getName




  def listFiles(path: SafePath, fileFilter: FileFilter, pluggedList: Seq[Plugin])(implicit workspace: Workspace): ListFilesData =
    given ServerFileSystemContext = path.context
    val treeNodesData = seqfileToSeqTreeNodeData(safePathToFile(path).listFilesSafe.toSeq, pluggedList)

    val sorted = treeNodesData.sorted(fileFilter.fileSorting)
    val nbFiles = treeNodesData.size

    fileFilter.firstLast match
      case FirstLast.First ⇒ ListFilesData(sorted, nbFiles)
      case FirstLast.Last ⇒ ListFilesData(sorted.reverse, nbFiles)


  def recursiveListFiles(path: SafePath, findString: Option[String])(implicit workspace: Workspace): Seq[(SafePath, Boolean)] = {
    given ServerFileSystemContext = path.context
    val fPath = safePathToFile(path).getAbsolutePath
    val allFiles = safePathToFile(path).recursiveListFilesSafe((f: File) => fPath != f.getAbsolutePath && findString.map(s => f.getName.contains(s)).getOrElse(true))
    allFiles.filter { case f ⇒ f.getName.contains(findString) }.map{f=> (fileToSafePath(f), f.isDirectory)}
  }


  def copyProjectFile(safePath: SafePath, newName: String, followSymlinks: Boolean = false)(implicit workspace: Workspace): SafePath = {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project

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



  def exists(safePath: SafePath)(implicit workspace: Workspace) = {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project
    safePathToFile(safePath).exists
  }

  def existsIn(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)
  }

//  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath])(implicit workspace: Workspace): Unit = {
//    val tmp: File = safePathToFile(tmpSafePath)(ServerFileSystemContext.absolute, workspace)
//
//    filesToBeMovedTo.foreach { f ⇒
//      val from = getFile(tmp, Seq(f.name))
//      val toFile: File = safePathToFile(f.parent)(ServerFileSystemContext.project, workspace)
//      copyFile(from, toFile)
//    }
//
//  }

//  def copyAllFromTmp(tmpSafePath: SafePath, to: SafePath)(implicit workspace: Workspace): Unit = {
//    val f: File = safePathToFile(tmpSafePath)
//    val toFile: File = safePathToFile(to)
//
//    val dirToCopy = {
//      val level1 = f.listFiles.toSeq
//      if (level1.size == 1) level1.head
//      else f
//    }
//
//    toFile.mkdir
//    dirToCopy.copy(toFile)
//
//  }

//  // Test if files exist in the 'to' directory, return the lists of already existing files or copy them otherwise
//  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
//
//  }

  //copy safePaths files to 'to' folder in overwriting in they exist
  def copyFilesTo(safePaths: Seq[SafePath], to: SafePath, overwrite: Boolean)(implicit workspace: Workspace): Seq[SafePath] =
    if(overwrite) {
      val existing = ListBuffer[SafePath]()
      safePaths.foreach { sp ⇒
        val destination = new File(to.toFile, sp.name)
        if(destination.exists()) existing.append(sp)
        sp.toFile.copy(destination)
      }
      existing.toSeq
    } else {
      val existing = existsIn(safePaths, to)
      if (existing.isEmpty) safePaths.foreach { sp ⇒ SafePathDecorator(sp).copy(to) }
      existing
    }

  def deleteFile(safePath: SafePath)(implicit workspace: Workspace): Unit = {
    implicit val ctx = safePath.context
    safePathToFile(safePath).recursiveDelete
  }

  def deleteFiles(safePaths: Seq[SafePath])(implicit workspace: Workspace): Unit = {
    safePaths.foreach { sp ⇒
      deleteFile(sp)
    }
  }

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
          inputSrc foreach { (c: Char) ⇒ ostream.write(c) }
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

  def authenticationKeysDirectory(implicit workspace: Workspace) = workspace.persistentDir / "keys"

//  def addPlugins(safePaths: Seq[SafePath])(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] = {
//    import org.openmole.gui.shared.data.ServerFileSystemContext.project
//    val files: Seq[File] = safePaths.map {
//      safePathToFile
//    }
//    addFilePlugins(files)
//  }
//
//  def addFilePlugins(files: Seq[File])(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] = {
//    val errors = org.openmole.core.module.addPluginsFiles(files, false, org.openmole.core.module.pluginDirectory)
//    errors.map(e ⇒ ErrorData(e._2))
//  }

  def addPlugin(safePath: SafePath)(implicit workspace: Workspace, newFile: TmpDirectory): Seq[ErrorData] = {

    import org.openmole.gui.shared.data.ServerFileSystemContext.project

    val file = safePathToFile(safePath)
    val errors = org.openmole.core.pluginmanager.PluginManager.tryLoad(Seq(file))
    errors.map(e ⇒ ErrorData(e._2)).toSeq
  }

  def removePlugin(safePath: SafePath)(implicit workspace: Workspace): Unit = synchronized {
    import org.openmole.gui.shared.data.ServerFileSystemContext.project
    import org.openmole.core.module
    val file: File = safePathToFile(safePath)
    val bundle = PluginManager.bundle(file)
    bundle.foreach(PluginManager.remove)
  }

  def formatDate(t: Long) = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(t)

}
