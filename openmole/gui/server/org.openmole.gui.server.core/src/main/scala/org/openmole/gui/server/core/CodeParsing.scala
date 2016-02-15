package org.openmole.gui.server.core

import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.DataUtils._
import scala.io.Source
import org.openmole.gui.server.core.Utils._

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

  def fromCommand(command: Seq[String]) = {
    val (language, codeName, commandElements: Seq[CommandElement]) = command.headOption match {
      case Some("python") ⇒ (Some(PythonLanguage()), command.lift(1).getOrElse(""), mapToVariableElements(indexArgs(command.drop(2), Seq()), CareTaskType()))
      case Some("R")      ⇒ (Some(RLanguage()), "", rParsing(command.drop(1), CareTaskType()))
      case _              ⇒ (None, command.head, command.drop(1))
    }

    //Parse the arguments and return the LaunchingCommand

    Some(
      BasicLaunchingCommand(
        language,
        codeName,
        commandElements)
    )
  }

  def fromFile(safePath: SafePath) = {
    safePath.name.split(".").last match {
      case "nlogo" ⇒ Some(netlogoParsing(safePath))
      case _       ⇒ None
    }
  }

  private def isFileString(fs: Option[String]) = fs match {
    case Some(s: String) ⇒ s matches ("""((.*)[.]([^.]+))|(.*/.*)""")
    case _               ⇒ false
  }

  private def indexArgs(args: Seq[String], indexed: Seq[(String, Option[String], Int)]): Seq[(String, Option[String], Int)] = {
    if (args.isEmpty) indexed
    else {
      val head = args.head
      val nextIndex = indexed.lastOption.map {
        _._3
      }.getOrElse(-1) + 1
      if (head.startsWith("-")) {
        val value = {
          if (args.size >= 2) {
            if (!args(1).startsWith("-")) Some(args(1)) else None
          }
          else None
        }
        indexArgs(args.drop(2), indexed :+ (keyName(Some(head), value, nextIndex), value, nextIndex)
        )
      }
      else indexArgs(args.drop(1), indexed :+ (keyName(None, Some(head), nextIndex), Some(head), nextIndex))
    }
  }

  def keyName(key: Option[String], value: Option[String], index: Int): String = {
    val isFile = isFileString(value)
    key match {
      case Some(k: String) ⇒ k
      case _ ⇒ if (isFile) {
        value match {
          case Some(v: String) ⇒ v.split('/').last.split('.').head
          case _               ⇒ "i" + index
        }
      }
      else "i" + index
    }
  }

  def renameDoublon(args: Seq[(String, Option[String], Int)]) = {
    val (doubled, fine) = args.groupBy {
      _._1
    }.partition { case (k, v) ⇒ v.size > 1 }
    fine.values.flatten ++ doubled.values.flatten.zipWithIndex.map { case (k, i) ⇒ (k._1 + "_" + i, k._2, k._3) }
  }

  private def mapToVariableElements(args: Seq[(String, Option[String], Int)], taskType: TaskType) = {
    renameDoublon(args).map { case (k, v, i) ⇒ toVariableElement(k, v, i, taskType) }
  }

  private def toVariableElement(key: String, value: Option[String], index: Int, taskType: TaskType): VariableElement = {
    val isFile = isFileString(value)
    VariableElement(index, ProtoTypePair(key,
      value match {
        case Some(a: String) ⇒ if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE
        case _               ⇒ ProtoTYPE.DOUBLE
      },
      if (isFile) "" else value.getOrElse(""),
      if (isFile) value else None
    ),
      taskType
    )
  }

  private def rParsing(args: Seq[String], taskType: TaskType): Seq[CommandElement] = {
    val indexed = indexArgs(args, Seq())

    val (others, toBeParsed) = indexed.partition { p ⇒
      Seq("--slave", "--args").contains(p._1)
    }

    val (f, rest) = toBeParsed.partition { p ⇒ p._1 == Some("-f") }

    mapToVariableElements(rest, taskType).toSeq ++ others.map { case (k, _, i) ⇒ StaticElement(i, k) } ++ f.map { x ⇒ StaticElement(x._3, "-f " + x._2.getOrElse("")) }
  }

  def netlogoParsing(safePath: SafePath): LaunchingCommand = {
    import org.openmole.gui.ext.data.ServerFileSytemContext.project
    val lines = Source.fromFile(safePath).getLines.toArray

    def parse(lines: Seq[(String, Int)], args: Seq[ProtoTypePair], outputs: Seq[ProtoTypePair]): (Seq[ProtoTypePair], Seq[ProtoTypePair]) = {
      if (lines.isEmpty) (args, outputs)
      else {
        val (line, index) = lines.head
        val tail = lines.tail
        if (line.startsWith("SLIDER")) parse(tail, args :+ parseSlider(index), outputs)
        else if (line.startsWith("SWITCH")) parse(tail, args :+ parseSwitch(index), outputs)
        else if (line.startsWith("INPUTBOX")) parse(tail, args :+ parseInputBox(index), outputs)
        else if (line.startsWith("CHOOSER")) parse(tail, args :+ parseChooser(index), outputs)
        else if (line.startsWith("MONITOR")) parse(tail, args, outputs ++ parseMonitor(index))
        // else if (line.startsWith("PLOT")) parse(tail, args, outputs ++ parsePlot(index))
        else parse(tail, args, outputs)
      }
    }

    def parseSlider(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 9), Some(name))
    }

    def parseSwitch(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.BOOLEAN, lines(start + 7), Some(name))
    }

    def parseInputBox(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 6), Some(name))
    }

    def parseMonitor(start: Int): Seq[ProtoTypePair] = {
      val name = lines(start + 6).split(' ')
      if (name.size == 1) Seq(ProtoTypePair(name.head.clean, ProtoTYPE.DOUBLE, mapping = Some(name.head)))
      else Seq()
    }

    def parseChooser(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.STRING, lines(start + 7).split(' ').head, Some(name))
    }

    val (args, outputs) = parse(lines.toSeq.zipWithIndex, Seq(), Seq())

    BasicLaunchingCommand(
      Some(NetLogoLanguage()), "",
      args.distinct.zipWithIndex.map { case (a, i) ⇒ VariableElement(i, a, NetLogoTaskType()) },
      outputs.distinct.zipWithIndex.map { case (o, i) ⇒ VariableElement(i, o, NetLogoTaskType()) }
    )
  }

}
