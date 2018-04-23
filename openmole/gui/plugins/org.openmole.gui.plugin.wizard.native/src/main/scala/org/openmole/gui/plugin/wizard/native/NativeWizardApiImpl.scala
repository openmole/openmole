/**
 * Created by Mathieu Leclaire on 19/04/18.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmole.gui.plugin.wizard.native

import java.io.{ ByteArrayInputStream, RandomAccessFile }
import java.nio.channels.FileChannel
import java.util.zip.GZIPInputStream

import org.openmole.core.services._
import org.openmole.core.workspace.Workspace
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.server.WizardUtils._
import org.openmole.tool.file.File
import org.openmole.tool.stream.StringOutputStream
import org.openmole.tool.tar.TarInputStream
import org.openmole.tool.stream._
import resource.{ managed, _ }
import org.openmole.gui.ext.tool.server.Utils
import org.openmole.gui.ext.tool.server.Utils._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.DataUtils._

class NativeWizardApiImpl(s: Services) extends NativeWizardAPI {

  def toTask(
    target:         SafePath,
    executableName: String,
    command:        String,
    inputs:         Seq[ProtoTypePair],
    outputs:        Seq[ProtoTypePair],
    libraries:      Option[String],
    resources:      Resources): SafePath = {

    val data = wizardModelData(inputs, outputs, resources, Some("netLogoInputs"), Some("netLogoOutputs"))
    val task = s"${executableName.split('.').head.toLowerCase}Task"

    val content = data.vals +
      s"""\n\nval $task = CARETask(workDirectory / "$executableName", "$command") set(\n""" +
      data.inputs + data.outputs + data.inputFileMapping + data.outputFileMapping + data.defaults +
      s""")\n\n$task hook ToStringHook()"""

    target.write(content)(context = org.openmole.gui.ext.data.ServerFileSystemContext.project, workspace = Workspace.instance)
    target
  }

  def parse(safePath: SafePath): Option[LaunchingCommand] = {

    def isFileString(fs: String): Boolean = fs matches ("""((.*)[.]([^.]+))|(.*/.*)""")

    case class IndexedArg(key: String, values: Seq[String], index: Int)

    def nbArgsUntilNextDash(args: Seq[String]): Int = args.zipWithIndex.find {
      _._1.startsWith("-")
    } match {
      case Some((arg: String, ind: Int)) ⇒ ind
      case _                             ⇒ args.length
    }

    def keyName(key: Option[String], values: Seq[String], index: Int): String = {
      val isFile = isFileString(values.headOption.getOrElse(""))
      key match {
        case Some(k: String) ⇒ k
        case _ ⇒ if (isFile) {
          values.headOption.map { v ⇒ v.split('/').last.split('.').head }.getOrElse("i" + index)
        }
        else "i" + index
      }
    }

    def indexArgs(args: Seq[String], indexed: Seq[IndexedArg]): Seq[IndexedArg] = {
      if (args.isEmpty) indexed
      else {
        val head = args.head
        val nextIndex = indexed.lastOption.map {
          _.index
        }.getOrElse(-1) + 1
        if (head.startsWith("-")) {
          //find next - option
          val nbArg = nbArgsUntilNextDash(args.tail)
          //get all next corresponding values
          val values = args.toList.slice(1, nbArg + 1).toSeq
          indexArgs(args.drop(nbArg + 1), indexed :+ IndexedArg(keyName(Some(head), values, nextIndex), values, nextIndex))
        }
        else {
          indexArgs(args.drop(1), indexed :+ IndexedArg(keyName(None, Seq(head), nextIndex), Seq(head), nextIndex))
        }
      }
    }

    def renameDoublon(args: Seq[IndexedArg]) = {
      val (doubled, fine) = args.groupBy {
        _.key
      }.partition {
        case (k, v) ⇒ v.size > 1
      }
      fine.values.flatten ++ doubled.values.flatten.zipWithIndex.map {
        case (k, i) ⇒ IndexedArg(k.key + "_" + i, k.values, k.index)
      }
    }

    def mapToVariableElements(args: Seq[IndexedArg], taskType: TaskType) =
      renameDoublon(args).flatMap {
        a ⇒ toVariableElements(a.key, a.values, a.index, taskType)
      }

    def toVariableElements(key: String, values: Seq[String], index: Int, taskType: TaskType): Seq[CommandElement] = {
      {
        if (key.startsWith("-")) Seq(StaticElement(index, key)) else Seq()
      } ++
        values.zipWithIndex.map {
          case (value, valIndex) ⇒
            val isFile = isFileString(value)
            VariableElement(index, ProtoTypePair(
              key.replaceAll("-", "") + {
                if (values.length > 1) valIndex + 1 else ""
              },
              if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE,
              if (isFile) "" else value,
              if (isFile) Some(value) else None
            ),
              taskType)
        }
    }

    def managedArchive(careArchive: File) = managed(new RandomAccessFile(careArchive, "r")) map (_.getChannel)

    def extractArchiveStream(archive: ManagedResource[FileChannel]) = archive.map { fileChannel ⇒

      //Get the tar.gz or the tgz from the bin archive
      val archiveSize = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileChannel.size - 8L, 8L).getLong.toInt
      fileChannel.position(0L)
      val srcArray = new Array[Byte](archiveSize)

      // Take final 100 and find I_LOVE PIZZA
      val pizza = "I_LOVE_PIZZA".getBytes
      val final100 = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileChannel.size - 100L, 100L)
      val array100 = new Array[Byte](100)
      final100.get(array100, 0, 100)
      val offset = 100L - array100.indexOfSlice(pizza).toLong

      fileChannel.map(FileChannel.MapMode.READ_ONLY, fileChannel.size - offset - archiveSize, archiveSize).get(srcArray, 0, archiveSize)

      //Extract and uncompress the tar.gz
      val stream = managed(new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(srcArray))))
      stream
    }.opt.get

    case class CAREInfo(commandLine: Option[Seq[String]])

    def fromCareArchive(careArchive: java.io.File): CAREInfo = fromArchiveStream(extractArchiveStream(managedArchive(careArchive)))

    /** The .opt.get at the end will force all operations to happen and close the managed resources */
    def fromArchiveStream(extractedArchiveStream: ManagedResource[TarInputStream]): CAREInfo =
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

    //  val (language, codeName, commandElements) =
    val command = fromCareArchive(safePathToFile(safePath)(
      context = org.openmole.gui.ext.data.ServerFileSystemContext.project,
      workspace = org.openmole.core.workspace.Workspace.instance)).commandLine.getOrElse(Seq())
    val (language, codeName, commandElements) = command.headOption match {
      case Some("python") ⇒ (Some(PythonLanguage()), command.lift(1).getOrElse(""), mapToVariableElements(indexArgs(command.drop(2), Seq()), CareTaskType()).toSeq)
      //  case Some("R")      ⇒ (Some(RLanguage()), "", "", RTaskType())
      //  case Some("java")   ⇒ (Some(JavaLikeLanguage()), "", rParsing(command.drop(1), CareTaskType()))
      case _              ⇒ (None, command.head, command.drop(1).zipWithIndex.map(e ⇒ StaticElement(e._2, e._1)))
    }

    //Parse the arguments and return the LaunchingCommand
    Some(
      BasicLaunchingCommand(
        language,
        codeName,
        commandElements
      )
    )
  }

}