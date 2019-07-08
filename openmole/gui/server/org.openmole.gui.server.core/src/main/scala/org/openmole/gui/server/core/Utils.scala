package org.openmole.gui.server.core

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.{ File, _ }
import java.util.logging.Level
import java.util.zip._

import scala.collection.JavaConversions.enumerationAsScalaIterator
import org.openmole.core.pluginmanager.{ PluginInfo, PluginManager }
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.gui.ext.data
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.ListSorting._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.file._
import org.openmole.core.fileservice._
import java.nio.file.attribute._

import org.openmole.gui.ext.plugin.server.PluginActivator
import org.openmole.gui.ext.tool.server.OMRouter
import org.openmole.gui.server.jscompile.JSPack

import scala.io.{ BufferedSource, Codec }
import org.openmole.core.services._
import org.openmole.core.pluginmanager.KeyWord

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }

object Utils extends JavaLogger {

  import Log._

  implicit def fileToExtension(f: File): FileExtension = DataUtils.fileToExtension(f.getName)

  def pluginUpdoadDirectory()(implicit workspace: Workspace) = workspace.tmpDir / "pluginUpload"

  def webUIDirectory()(implicit workspace: Workspace) = workspace.location /> "webui"

  def projectsDirectory()(implicit workspace: Workspace) = webUIDirectory /> "projects"

  def workspaceRoot()(implicit workspace: Workspace) = workspace.location

  def isPlugin(path: SafePath)(implicit workspace: Workspace): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    !PluginManager.listBundles(safePathToFile(path)).isEmpty
  }

  def allPluggableIn(path: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    path.listFiles().filter { f ⇒
      PluginManager.isBundle(f)
    }.toSeq
  }

  def treeNodeToSafePath(tnd: TreeNodeData, parent: SafePath): SafePath = parent ++ tnd.name

  implicit def fileToSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): SafePath = {
    context match {
      case _: ProjectFileSystem ⇒ SafePath(getPathArray(f, projectsDirectory))
      case _                    ⇒ SafePath(getPathArray(f, new File("")))
    }
  }

  implicit def safePathToFile(s: SafePath)(implicit context: ServerFileSystemContext, workspace: Workspace): File = {
    context match {
      case _: ProjectFileSystem ⇒ getFile(webUIDirectory, s.path)
      case _                    ⇒ getFile(new File(""), s.path)
    }

  }

  implicit def seqOfSafePathToSeqOfFile(s: Seq[SafePath])(implicit context: ServerFileSystemContext, workspace: Workspace): Seq[File] = s.map {
    safePathToFile
  }

  implicit def seqOfFileToSeqOfSafePath(s: Seq[File])(implicit context: ServerFileSystemContext, workspace: Workspace): Seq[SafePath] = s.map {
    fileToSafePath
  }

  def fileToTreeNodeData(f: File)(implicit context: ServerFileSystemContext = ProjectFileSystem(), workspace: Workspace, services: Services): Option[TreeNodeData] = {
    val time = if (f.exists) Some(
      java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
    )
    else None

    val dirData =
      if (f.isDirectory) Some(DirData(f.isDirectoryEmpty, rootVersionedDirectory(f).map { _ ⇒ Versioning() }))
      else None

    time.map(t ⇒ TreeNodeData(f.getName, dirData, f.length, t))
  }

  implicit def safePathToTreeNodeData(safePath: SafePath)(implicit context: ServerFileSystemContext = ProjectFileSystem(), workspace: Workspace, services: Services): Option[TreeNodeData] = fileToTreeNodeData(safePathToFile(safePath))

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File])(implicit context: ServerFileSystemContext, workspace: Workspace, services: Services): Seq[TreeNodeData] = fs.flatMap {
    fileToTreeNodeData(_)(context, workspace, services)
  }

  implicit def fileToOptionSafePath(f: File)(implicit context: ServerFileSystemContext, workspace: Workspace): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel = {
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorLevel()
    else DebugLevel()
  }

  implicit class SafePathDecorator(sp: SafePath) {

    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    def copy(toPath: SafePath, withName: Option[String] = None)(implicit workspace: Workspace) = {
      val from: File = sp
      val to: File = toPath
      if (from.exists && to.exists) {
        FileDecorator(from).copy(toF = new File(to, withName.getOrElse(from.getName)), followSymlinks = true)
      }
    }
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

  def rootVersionedDirectory(file: File)(implicit workspace: Workspace): Option[File] = {
    file match {
      case (f: File) ⇒
        if (f == projectsDirectory()) None
        else if (f.isDirectory) {
          if (f.listFiles.exists {
            _.getName == ".git"
          }) Some(f)
          else rootVersionedDirectory(file.getParentFile)
        }
        else rootVersionedDirectory(file.getParentFile)
      case _ ⇒ None
    }
  }

  def toVersionedTuple(treeNodeDatas: Seq[TreeNodeData], modifiedFileNames: Seq[String], parent: SafePath)(implicit context: ServerFileSystemContext, workspace: Workspace) = {

    treeNodeDatas.map { tnd ⇒
      VersionedTreeNodeData(tnd, modifiedFileNames.contains(tnd.name) match {
        case true ⇒
          val safePath = treeNodeToSafePath(tnd, parent)
          rootVersionedDirectory(safePathToFile(safePath)).map { repo ⇒
            val content = org.openmole.tool.version.content(repo, safePath)
            Modified(content)
          }.getOrElse(Modified(""))
        case _ ⇒ Clear()
      })
    }
  }

  def listFiles(path: SafePath, fileFilter: data.FileFilter)(implicit context: ServerFileSystemContext, workspace: Workspace, services: Services): ListFilesData = {

    val allFiles = safePathToFile(path).listFilesSafe.toSeq

    val filteredByName: Seq[TreeNodeData] = {
      if (fileFilter.nameFilter.isEmpty) allFiles
      else allFiles.filter { f ⇒ f.getName.contains(fileFilter.nameFilter) }
    }

    val sorted = filteredByName.sorted(fileFilter.fileSorting)
    val threshold = fileFilter.threshold.getOrElse(1000)
    val nbFiles = allFiles.size

    val parentFile: File = path
    val modifiedFiles = rootVersionedDirectory(parentFile).flatMap { r ⇒
      r.flatMap { root ⇒
        val rootSafePath: SafePath = root
        PluginActivator.versioningApi.headOption.map { factory ⇒ factory(services).modifiedFiles(rootSafePath) }
      }
    }.getOrElse(Seq()).map {
      _.name
    }

    fileFilter.firstLast match {
      case First() ⇒ ListFilesData(toVersionedTuple(sorted.take(threshold), modifiedFiles, path), nbFiles)
      case Last()  ⇒ ListFilesData(toVersionedTuple(sorted.takeRight(threshold).reverse, modifiedFiles, path), nbFiles)
    }
  }

  def copy(safePath: SafePath, newName: String, followSymlinks: Boolean = false)(implicit workspace: Workspace): SafePath = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project

    val toPath = safePath.copy(path = safePath.path.dropRight(1) :+ newName)
    if (toPath.isDirectory()) toPath.mkdir

    val from: File = safePath
    val replica: File = safePath.parent ++ newName
    FileDecorator(from).copy(replica, followSymlinks = followSymlinks)

    replica
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

  def copy(from: File, to: File): Unit =
    if (from.exists && to.exists) {
      from.copy(new File(to, from.getName))
    }

  def exists(safePath: SafePath)(implicit workspace: Workspace) = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    safePathToFile(safePath).exists
  }

  def existsExcept(in: SafePath, exceptItSelf: Boolean)(implicit workspace: Workspace, services: Services): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSystemContext.project
    val li = listFiles(in.parent, data.FileFilter.defaultFilter)
    val count = li.list.count(l ⇒ treeNodeToSafePath(l.treeNodeData, in.parent).path == in.path)

    val bound = if (exceptItSelf) 1 else 0
    if (count > bound) true else false
  }

  def existsIn(safePaths: Seq[SafePath], to: SafePath)(implicit workspace: Workspace): Seq[SafePath] = {
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)
  }

  def copyToPluginUploadDirectory(safePaths: Seq[SafePath])(implicit workspace: Workspace) = {
    safePaths.map { sp ⇒
      val from = safePathToFile(sp)(ServerFileSystemContext.project, workspace)
      pluginUpdoadDirectory.mkdirs
      copy(from, pluginUpdoadDirectory)
    }
  }

  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath])(implicit workspace: Workspace): Unit = {
    val tmp: File = safePathToFile(tmpSafePath)(ServerFileSystemContext.absolute, workspace)

    filesToBeMovedTo.foreach { f ⇒
      val from = getFile(tmp, Seq(f.name))
      val toFile: File = safePathToFile(f.parent)(ServerFileSystemContext.project, workspace)
      copy(from, toFile)
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
    safePaths.foreach { sp ⇒ sp.copy(to) }

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

  def updateJsPluginDirectory(jsPluginDirectory: File) = {
    jsPluginDirectory.recursiveDelete
    jsPluginDirectory.mkdirs
    Plugins.gatherJSIRFiles(jsPluginDirectory)
    jsPluginDirectory
  }

  val openmoleFileName = "openmole.js"
  val depsFileName = "deps.js"
  val openmoleGrammarName = "openmole_grammar_template.js"
  val openmoleGrammarMode = "mode-openmole.js"

  def updateIfChanged(file: File)(update: File ⇒ Unit)(implicit fileService: FileService, newFile: NewFile) = {
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

  def openmoleFile(optimizedJS: Boolean)(implicit workspace: Workspace, newFile: NewFile, fileService: FileService) = {
    val jsPluginDirectory = webUIDirectory / "jsplugin"
    updateJsPluginDirectory(jsPluginDirectory)

    val jsFile = workspace.persistentDir /> "webui" / openmoleFileName

    def update = {
      logger.info("Building GUI plugins ...")
      jsFile.delete
      JSPack.link(jsPluginDirectory, jsFile, optimizedJS)
    }

    (jsPluginDirectory / "optimized_mode").content = optimizedJS.toString

    if (!jsFile.exists) update
    else updateIfChanged(jsPluginDirectory) { _ ⇒ update }
    jsFile
  }

  def expandDepsFile(template: File, to: File) = {
    val rules = PluginInfo.keyWords.partition { kw ⇒
      kw match {
        case _@ (KeyWord.TaskKeyWord(_) | KeyWord.SourceKeyWord(_) | KeyWord.EnvironmentKeyWord(_) | KeyWord.HookKeyWord(_) | KeyWord.SamplingKeyWord(_) | KeyWord.DomainKeyWord(_) | KeyWord.PatternKeyWord(_)) ⇒ false
        case _ ⇒ true
      }
    }

    to.content =
      s"""${template.content}""" // ${AceOpenMOLEMode.content}
        .replace(
          "##OMKeywords##",
          s""" "${
            rules._1.map {
              _.name
            }.mkString("|")
          }" """)
        .replace(
          "##OMClasses##",
          s""" "${
            rules._2.map {
              _.name
            }.mkString("|")
          }" """)

  }

  def addPluginRoutes(route: OMRouter ⇒ Unit, services: Services) = {
    logger.info("Loading GUI plugins")
    PluginActivator.plugins.foreach { p ⇒ route(p._2.router(services)) }
  }

  // Extract .zip archive
  def unzip(from: File, to: File) = {
    val basename = from.getName.substring(0, from.getName.lastIndexOf("."))
    to.getParentFile.mkdirs

    val zip = new ZipFile(from)
    zip.entries.foreach { entry ⇒
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

  def catchAll[T](f: ⇒ T): Try[T] = {
    val res =
      try Success(f)
      catch {
        case t: Throwable ⇒ Failure(t)
      }
    res
  }

}
