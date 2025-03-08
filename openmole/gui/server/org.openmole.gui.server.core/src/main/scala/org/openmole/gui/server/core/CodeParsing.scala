package org.openmole.gui.server.core

import org.openmole.core.workspace.Workspace
import org.openmole.gui.shared.data.*

import scala.io.Source

/*
 * Copyright (C) 10/11/15 // mathieu.leclaire@openmole.org
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

object CodeParsing {

  //  def fromCommand(command: Seq[String]) = {
  //    val (language, codeName, commandElements) = command.headOption match {
  //      case Some("python") => (Some(PythonLanguage()), command.lift(1).getOrElse(""), mapToVariableElements(indexArgs(command.drop(2), Seq()), CareTaskType()).toSeq)
  //      //  case Some("R")      => (Some(RLanguage()), "", "", RTaskType())
  //      //  case Some("java")   => (Some(JavaLikeLanguage()), "", rParsing(command.drop(1), CareTaskType()))
  //      case _              => (None, command.head, command.drop(1).zipWithIndex.map(e => StaticElement(e._2, e._1)))
  //    }
  //
  //    //Parse the arguments and return the LaunchingCommand
  //    Some(
  //      BasicLaunchingCommand(
  //        language,
  //        codeName,
  //        commandElements
  //      )
  //    )
  //  }
  //
  //  def fromFile(safePath: SafePath)(implicit workspace: Workspace) = {
  //    safePath.name.split(".").last match {
  //      case "nlogo" => Some(netlogoParsing(safePath))
  //      case _       => None
  //    }
  //  }

  //  private def isFileString(fs: String) = fs matches ("""((.*)[.]([^.]+))|(.*/.*)""")
  //
  //  case class IndexedArg(key: String, values: Seq[String], index: Int)
  //
  //  private def nbArgsUntilNextDash(args: Seq[String]): Int = args.zipWithIndex.find {
  //    _._1.startsWith("-")
  //  } match {
  //    case Some((arg: String, ind: Int)) => ind
  //    case _                             => args.length
  //  }
  //
  //  private def indexArgs(args: Seq[String], indexed: Seq[IndexedArg]): Seq[IndexedArg] = {
  //    if (args.isEmpty) indexed
  //    else {
  //      val head = args.head
  //      val nextIndex = indexed.lastOption.map {
  //        _.index
  //      }.getOrElse(-1) + 1
  //      if (head.startsWith("-")) {
  //        //find next - option
  //        val nbArg = nbArgsUntilNextDash(args.tail)
  //        //get all next corresponding values
  //        val values = args.toList.slice(1, nbArg + 1).toSeq
  //        indexArgs(args.drop(nbArg + 1), indexed :+ IndexedArg(keyName(Some(head), values, nextIndex), values, nextIndex))
  //      }
  //      else {
  //        indexArgs(args.drop(1), indexed :+ IndexedArg(keyName(None, Seq(head), nextIndex), Seq(head), nextIndex))
  //      }
  //    }
  //  }

  //  def keyName(key: Option[String], values: Seq[String], index: Int): String = {
  //    val isFile = isFileString(values.headOption.getOrElse(""))
  //    key match {
  //      case Some(k: String) => k
  //      case _ => if (isFile) {
  //        values.headOption.map { v => v.split('/').last.split('.').head }.getOrElse("i" + index)
  //      }
  //      else "i" + index
  //    }
  //  }

  //  def renameDoublon(args: Seq[IndexedArg]) = {
  //    val (doubled, fine) = args.groupBy {
  //      _.key
  //    }.partition {
  //      case (k, v) => v.size > 1
  //    }
  //    fine.values.flatten ++ doubled.values.flatten.zipWithIndex.map {
  //      case (k, i) => IndexedArg(k.key + "_" + i, k.values, k.index)
  //    }
  //  }
  //
  //  private def mapToVariableElements(args: Seq[IndexedArg], taskType: TaskType) =
  //    renameDoublon(args).flatMap {
  //      a => toVariableElements(a.key, a.values, a.index, taskType)
  //    }
  //
  //  private def toVariableElements(key: String, values: Seq[String], index: Int, taskType: TaskType): Seq[CommandElement] = {
  //    {
  //      if (key.startsWith("-")) Seq(StaticElement(index, key)) else Seq()
  //    } ++
  //      values.zipWithIndex.map {
  //        case (value, valIndex) =>
  //          val isFile = isFileString(value)
  //          VariableElement(index, PrototypePair(
  //            key.replaceAll("-", "") + { if (values.length > 1) valIndex + 1 else "" },
  //            if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE,
  //            if (isFile) "" else value,
  //            if (isFile) Some(value) else None
  //          ),
  //            taskType)
  //      }
  //  }
  //
  //  private def rParsing(args: Seq[String], taskType: TaskType): Seq[CommandElement] = {
  //    val indexed = indexArgs(args, Seq())
  //    val (others, toBeParsed) = indexed.partition {
  //      p => Seq("--slave").contains(p.key)
  //    }
  //    val (f, rest) = toBeParsed.partition {
  //      p => p.key == "-f"
  //    }
  //
  //    mapToVariableElements(rest, taskType).toSeq ++ others.map {
  //      o => StaticElement(o.index, o.key)
  //    } ++ f.map {
  //      x => StaticElement(x.index, "-f " + x.values.mkString(" "))
  //    }
  //  }

  //  def netlogoParsing(safePath: SafePath)(implicit workspace: Workspace): LaunchingCommand = {
  //
  //    import org.openmole.gui.shared.data.ServerFileSystemContext.project
  //
  //    val lines = Source.fromFile(safePath).getLines.toArray
  //
  //    def parse(lines: Seq[(String, Int)], args: Seq[PrototypePair], outputs: Seq[PrototypePair]): (Seq[PrototypePair], Seq[PrototypePair]) = {
  //      if (lines.isEmpty) (PrototypePair("seed", ProtoTYPE.INT, "0", None) +: args, outputs)
  //      else {
  //        val (line, index) = lines.head
  //        val tail = lines.tail
  //        if (line.startsWith("SLIDER")) parse(tail, args :+ parseSlider(index), outputs)
  //        else if (line.startsWith("SWITCH")) parse(tail, args :+ parseSwitch(index), outputs)
  //        else if (line.startsWith("INPUTBOX")) parse(tail, args :+ parseInputBox(index), outputs)
  //        else if (line.startsWith("CHOOSER")) parse(tail, args :+ parseChooser(index), outputs)
  //        else if (line.startsWith("MONITOR")) parse(tail, args, outputs ++ parseMonitor(index))
  //        // else if (line.startsWith("PLOT")) parse(tail, args, outputs ++ parsePlot(index))
  //        else parse(tail, args, outputs)
  //      }
  //    }
  //
  //    def parseSlider(start: Int): PrototypePair = {
  //      val name = lines(start + 5)
  //      PrototypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 9), Some(name))
  //    }
  //
  //    def parseSwitch(start: Int): PrototypePair = {
  //      val name = lines(start + 5)
  //      PrototypePair(name.clean, ProtoTYPE.BOOLEAN, lines(start + 7), Some(name))
  //    }
  //
  //    def parseInputBox(start: Int): PrototypePair = {
  //      val name = lines(start + 5)
  //      PrototypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 6), Some(name))
  //    }
  //
  //    def parseMonitor(start: Int): Seq[PrototypePair] = {
  //      val name = lines(start + 6).split(' ')
  //      if (name.size == 1) Seq(PrototypePair(name.head.clean, ProtoTYPE.DOUBLE, mapping = Some(name.head)))
  //      else Seq()
  //    }
  //
  //    def parseChooser(start: Int): PrototypePair = {
  //      val name = lines(start + 5)
  //      PrototypePair(name.clean, ProtoTYPE.STRING, lines(start + 7).split(' ').head, Some(name))
  //    }
  //
  //    val (args, outputs) = parse(lines.toSeq.zipWithIndex, Seq(), Seq())
  //
  //    BasicLaunchingCommand(
  //      Some(NetLogoLanguage()), "",
  //      args.distinct.zipWithIndex.map {
  //        case (a, i) => VariableElement(i, a, NetLogoTaskType())
  //      },
  //      outputs.distinct.zipWithIndex.map {
  //        case (o, i) => VariableElement(i, o, NetLogoTaskType())
  //      }
  //    )
  //  }

}
