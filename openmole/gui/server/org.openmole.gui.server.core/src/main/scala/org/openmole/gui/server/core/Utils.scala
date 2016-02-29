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

import java.io.File
import java.lang.reflect.Modifier
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Level
import java.util.zip.{ ZipInputStream, GZIPInputStream }
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data.{ FileFilter ⇒ fF }
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileSorting._
import java.io._
import org.openmole.tool.file._
import org.openmole.tool.stream.StringOutputStream
import org.openmole.tool.tar._
import java.nio.file.attribute._

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

object Utils {

  implicit def fileToExtension(f: File): FileExtension = DataUtils.fileToExtension(f.getName)

  val webUIProjectFile = Workspace.file("webui")

  def workspaceProjectFile = {
    val ws = new File(Workspace.file("webui"), "projects")
    ws.mkdirs()
    ws
  }

  def workspaceRoot = workspaceProjectFile.getParentFile

  def authenticationKeysFile = {
    val ak = Workspace.location / Workspace.persistentLocation / "keys"
    ak.mkdirs()
    ak
  }

  def isPlugin(path: SafePath): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    !PluginManager.plugins(safePathToFile(path)).isEmpty
  }

  implicit def fileToSafePath(f: File)(implicit context: ServerFileSytemContext): SafePath = {
    context match {
      case ProjectFileSystem ⇒ SafePath(getPathArray(f, workspaceProjectFile), f)
      case _                 ⇒ SafePath(getPathArray(f, new File("")), f)
    }
  }

  implicit def safePathToFile(s: SafePath)(implicit context: ServerFileSytemContext): File = {
    context match {
      case ProjectFileSystem ⇒ getFile(webUIProjectFile, s.path)
      case _                 ⇒ getFile(new File(""), s.path)
    }

  }

  implicit def seqOfSafePathToSeqOfFile(s: Seq[SafePath])(implicit context: ServerFileSytemContext): Seq[File] = s.map {
    safePathToFile
  }

  implicit def seqOfFileToSeqOfSafePath(s: Seq[File])(implicit context: ServerFileSytemContext): Seq[SafePath] = s.map {
    fileToSafePath
  }

  implicit def fileToTreeNodeData(f: File)(implicit context: ServerFileSytemContext = ProjectFileSystem): TreeNodeData = {
    val time = java.nio.file.Files.readAttributes(f, classOf[BasicFileAttributes]).lastModifiedTime.toMillis
    TreeNodeData(f.getName, f, f.isDirectory, f.length, {
      if (f.isFile) readableByteCount(FileDecorator(f).size) else ""
    }, time, (new SimpleDateFormat("dd/MM/yyyy HH:mm").format((new Date(time)))))
  }

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File])(implicit context: ServerFileSytemContext): Seq[TreeNodeData] = fs.map {
    fileToTreeNodeData(_)
  }

  implicit def fileToOptionSafePath(f: File)(implicit context: ServerFileSytemContext): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel = {
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorLevel()
    else DebugLevel()
  }

  implicit class SafePathDecorator(sp: SafePath) {

    import org.openmole.gui.ext.data.ServerFileSytemContext.project

    def copy(toPath: SafePath, withName: Option[String] = None) = {
      val from: File = sp
      val to: File = toPath
      if (from.exists && to.exists) {
        from.copy(new File(to, withName.getOrElse(from.getName)))
      }
    }
  }

  def authenticationFile(keyFileName: String): File = new File(authenticationKeysFile, keyFileName)

  def getPathArray(f: File, until: File): Seq[String] = {
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
    def getFile0(paths: Seq[String], accFile: File): File = {
      if (paths.isEmpty) accFile
      else getFile0(paths.tail, new File(accFile, paths.head))
    }
    getFile0(paths, root)
  }

  def listFiles(path: SafePath, fileFilter: fF)(implicit context: ServerFileSytemContext): Seq[TreeNodeData] = {

    val list = safePathToFile(path).listFilesSafe.toSeq

    val filteredByName: Seq[TreeNodeData] = {
      if (fileFilter.nameFilter.isEmpty) list
      else list.filter { f ⇒ f.getName.contains(fileFilter.nameFilter) }
    }

    val sorted = filteredByName.sorted(fileFilter.fileSorting)

    fileFilter.threshold.map { th ⇒
      fileFilter.firstLast match {
        case First ⇒ sorted.take(th)
        case _     ⇒ sorted.takeRight(th)
      }
    }.getOrElse(sorted)

  }

  def replicate(treeNodeData: TreeNodeData): TreeNodeData = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project

    val newName = {
      val prefix = treeNodeData.safePath.path.last
      if (treeNodeData.isDirectory) prefix + "_1"
      else prefix.replaceFirst("[.]", "_1.")
    }

    val toPath = treeNodeData.safePath.copy(path = treeNodeData.safePath.path.dropRight(1) :+ newName)
    if (toPath.isDirectory()) toPath.mkdir

    val parent = treeNodeData.safePath.parent
    treeNodeData.safePath.copy(treeNodeData.safePath.parent, Some(newName))

    val f: File = parent ++ newName
    f
  }

  def launchinCommands(model: SafePath): Seq[LaunchingCommand] = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    model.name.split('.').last match {
      case "nlogo" ⇒ Seq(CodeParsing.netlogoParsing(model))
      case "jar"   ⇒ Seq(JavaLaunchingCommand(JarMethod("", Seq(), "", true, ""), Seq(), Seq()))
      case _       ⇒ Seq(CodeParsing.fromCommand(getCareBinInfos(model).commandLine.getOrElse(Seq())).get)
    }
  }

  def jarClasses(jarPath: SafePath): Seq[ClassTree] = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    val zip = new ZipInputStream(new FileInputStream(jarPath))
    val classes = Stream.continually(zip.getNextEntry).
      takeWhile(_ != null).filter { e ⇒
        e.getName.endsWith(".class")
      }.filterNot { e ⇒
        Seq("scala", "java").exists {
          ex ⇒ e.getName.startsWith(ex)
        }
      }.map {
        _.getName.dropRight(6).split("/").toSeq
      }

    val trees = buildClassTrees(classes)
    zip.close
    trees
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
          else classTrees :+ ClassNode(k,
            build(v.map(_.tail), classTrees)
          )
      }.toSeq
    }

    build(classes, Seq())
  }

  def jarMethods(jarPath: SafePath, classString: String): Seq[JarMethod] = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    val classLoader = new URLClassLoader(Seq(jarPath.toURI.toURL), this.getClass.getClassLoader)
    val clazz = Class.forName(classString, true, classLoader)

    clazz.getDeclaredMethods.map { m ⇒
      JarMethod(m.getName, m.getGenericParameterTypes.map {
        _.toString.split("class ").last
      }.toSeq, m.getReturnType.getCanonicalName, Modifier.isStatic(m.getModifiers), classString)
    }
  }

  def move(from: File, to: File): Unit =
    if (from.exists && to.exists) {
      from.move(new File(to, from.getName))
    }

  def copy(from: File, to: File): Unit =
    if (from.exists && to.exists) {
      from.copy(new File(to, from.getName))
    }

  def exists(safePath: SafePath) = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    safePathToFile(safePath).exists
  }

  def existsExcept(in: TreeNodeData, exceptItSelf: Boolean): Boolean = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    val li = listFiles(in.safePath.parent, fF.defaultFilter)
    val count = li.count(_.safePath.path == in.safePath.path)

    val bound = if (exceptItSelf) 1 else 0
    if (count > bound) true else false
  }

  def existsIn(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath] = {
    safePaths.map { sp ⇒
      to ++ sp.name
    }.filter(exists)
  }

  def copyFromTmp(tmpSafePath: SafePath, filesToBeMovedTo: Seq[SafePath]): Unit = {
    val tmp: File = safePathToFile(tmpSafePath)(ServerFileSytemContext.absolute)

    filesToBeMovedTo.foreach { f ⇒
      val from = getFile(tmp, Seq(f.name))
      val toFile: File = safePathToFile(f.parent)(ServerFileSytemContext.project)
      copy(from, toFile)
    }

  }

  def copyAllTmpTo(tmpSafePath: SafePath, to: SafePath): Unit = {

    val f: File = safePathToFile(tmpSafePath)(ServerFileSytemContext.absolute)
    val toFile: File = safePathToFile(to)(ServerFileSytemContext.project)

    val dirToCopy = {
      val level1 = f.listFiles.toSeq
      if (level1.size == 1) level1.head
      else f
    }

    toFile.mkdir
    dirToCopy.copy(toFile)

  }

  // Test if files exist in the 'to' directory, return the lists of already existing files or copy them otherwise
  def testExistenceAndCopyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath): Seq[SafePath] = {
    val existing = existsIn(safePaths, to)

    if (existing.isEmpty) safePaths.foreach { sp ⇒ sp.copy(to) }
    existing
  }

  //copy safePaths files to 'to' folder in overwriting in they exist
  def copyProjectFilesTo(safePaths: Seq[SafePath], to: SafePath) = {
    safePaths.foreach { sp ⇒ sp.copy(to) }

  }

  def deleteFile(safePath: SafePath, context: ServerFileSytemContext): Unit = {
    implicit val ctx = context
    safePathToFile(safePath).recursiveDelete
  }

  def deleteFiles(safePaths: Seq[SafePath], context: ServerFileSytemContext): Unit = {
    safePaths.foreach { sp ⇒
      deleteFile(sp, context)
    }
  }

  import resource._

  def managedArchive(careArchive: File) = managed(new RandomAccessFile(careArchive, "r")) map (_.getChannel)

  def extractArchiveStream(archive: ManagedResource[FileChannel]) = archive.map { fileChannel ⇒
    //Get the tar.gz from the bin archive
    val endMinus8Bytes = fileChannel.size - 8L
    val archiveSize = fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes, 8L).getLong.toInt
    fileChannel.position(0L)
    val srcArray = new Array[Byte](archiveSize)
    fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes - 13L - archiveSize, archiveSize).get(srcArray, 0, archiveSize)

    //Extract and uncompress the tar.gz
    val stream = managed(new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(srcArray))))
    stream
  }.opt.get

  case class CAREInfo(commandLine: Option[Seq[String]])

  def getCareBinInfos(careArchive: File): CAREInfo = getCareBinInfos(extractArchiveStream(managedArchive(careArchive)))

  /** The .opt.get at the end will force all operations to happen and close the managed resources */
  def getCareBinInfos(extractedArchiveStream: ManagedResource[TarInputStream]) =
    extractedArchiveStream.map { stream ⇒

      Iterator.continually(stream.getNextEntry).dropWhile { te ⇒
        val pathString = te.getName.split("/")
        pathString.last != "re-execute.sh" || pathString.contains("rootfs")
      }.toSeq.headOption.flatMap { _ ⇒

        val linesManaged = managed(new StringOutputStream) map { stringW: StringOutputStream ⇒
          stream copy stringW
          stringW.toString.split("\n")
        }
        val lines = linesManaged.opt.get

        val prootLine = lines.indexWhere(s ⇒ s.startsWith("PROOT="))
        val commands =
          if (prootLine != -1) {
            // get only the command lines, and strip each component from its single quotes and final backslash
            Some(lines.slice(7, prootLine - 1).map { l ⇒ l.dropRight(2) }.map {
              _.drop(1)
            }.map {
              _.dropRight(1)
            }.toSeq)
          }
          else None

        Some(CAREInfo(commands))
      }.get
    }.opt.get

}
