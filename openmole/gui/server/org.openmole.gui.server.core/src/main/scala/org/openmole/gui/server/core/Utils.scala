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
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.util.logging.Level
import java.util.zip.{ ZipEntry, ZipInputStream, GZIPInputStream }
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileExtension._
import java.io._
import org.openmole.tool.file._
import org.openmole.tool.stream.StringOutputStream
import org.openmole.tool.tar._

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

object Utils {

  implicit def fileToExtension(f: File): FileExtension = f.getName match {
    case x if x.endsWith(".oms")                         ⇒ OMS
    case x if x.endsWith(".scala")                       ⇒ SCALA
    case x if x.endsWith(".sh")                          ⇒ SH
    case x if x.endsWith(".tgz") | x.endsWith(".tar.gz") ⇒ TGZ
    case x if x.endsWith(".csv") |
      x.endsWith(".nlogo") |
      x.endsWith(".gaml") |
      x.endsWith(".nls") |
      x.endsWith(".py") |
      x.endsWith(".R") |
      x.endsWith(".txt") ⇒ TEXT
    case x if x.endsWith(".md") ⇒ MD
    case _                      ⇒ BINARY
  }

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

  def isPlugin(path: SafePath): Boolean = !PluginManager.plugins(safePathToFile(path)).isEmpty

  implicit def fileToSafePath(f: File): SafePath = SafePath(getPathArray(f, workspaceProjectFile), f)

  implicit def safePathToFile(s: SafePath): File = getFile(webUIProjectFile, s.path)

  implicit def seqOfSafePathToSeqOfFile(s: Seq[SafePath]): Seq[File] = s.map {
    safePathToFile
  }

  implicit def fileToTreeNodeData(f: File): TreeNodeData = TreeNodeData(f.getName, f, f.isDirectory, isPlugin(f), f.length, readableByteCount(FileDecorator(f).size))

  implicit def seqfileToSeqTreeNodeData(fs: Seq[File]): Seq[TreeNodeData] = fs.map {
    fileToTreeNodeData(_)
  }

  implicit def fileToOptionSafePath(f: File): Option[SafePath] = Some(fileToSafePath(f))

  implicit def javaLevelToErrorLevel(level: Level): ErrorStateLevel = {
    if (level.intValue >= java.util.logging.Level.WARNING.intValue) ErrorLevel()
    else DebugLevel()
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

  def listFiles(path: SafePath): Seq[TreeNodeData] = safePathToFile(path).listFilesSafe.toSeq

  def launchinCommands(model: SafePath): Seq[LaunchingCommand] =
    model.name.split('.').last match {
      case "nlogo" ⇒ Seq(CodeParsing.netlogoParsing(model))
      case "jar"   ⇒ Seq(JavaLaunchingCommand(JarMethod("", Seq(), ""), Seq(), Seq()))
      case _       ⇒ Seq(getCareBinInfos(model)).flatten
    }

  private def getCareBinInfos(careArchive: SafePath): Option[LaunchingCommand] = {
    val fileChannel = new RandomAccessFile(careArchive, "r").getChannel

    try {
      //Get the tar.gz from the bin archive
      val endMinus8Bytes = fileChannel.size - 8L
      val archiveSize = fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes, 8L).getLong.toInt
      fileChannel.position(0L)
      val srcArray = new Array[Byte](archiveSize)
      fileChannel.map(FileChannel.MapMode.READ_ONLY, endMinus8Bytes - 13L - archiveSize, archiveSize).get(srcArray, 0, archiveSize)

      //Extract and uncompress the tar.gz
      val stream = new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(srcArray)))

      Iterator.continually(stream.getNextEntry).dropWhile { te ⇒
        val pathString = te.getName.split("/")
        pathString.last != "re-execute.sh" || pathString.contains("rootfs")
      }.toSeq.headOption.flatMap { e ⇒
        val stringW = new StringOutputStream
        stream copy stringW
        val lines = stringW.toString.split("\n")
        val prootLine = lines.indexWhere(s ⇒ s.startsWith("PROOT="))
        if (prootLine != -1) {
          val command = lines.slice(7, prootLine - 1).map { l ⇒ l.dropRight(2) }.map {
            _.drop(1)
          }.map {
            _.dropRight(1)
          }.toSeq
          CodeParsing.fromCommand(command)
        }
        else None
      }
    }
    finally {
      fileChannel.close
    }

  }

  def jarClasses(jarPath: SafePath): Seq[ClassTree] = {
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
    val classLoader = new URLClassLoader(Seq(jarPath.toURI.toURL), this.getClass.getClassLoader)
    val clazz = Class.forName(classString, true, classLoader)

    clazz.getDeclaredMethods.map { m ⇒
      JarMethod(m.getName, m.getGenericParameterTypes.map {
        _.toString.split("class ").last
      }.toSeq, m.getReturnType.getCanonicalName)
    }
  }

}
