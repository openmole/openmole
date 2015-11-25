package org.openmole.gui.server.core

import org.openmole.gui.ext.data._

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

  //implicit def stringsToCommandArguments(seqS: Seq[String]): Seq[CommandArgument] = defaultParseArgs(seqS, Seq())

  def fromCommand(command: Seq[String]) = {
    val (language, codeName, commandElements: Seq[CommandElement]) = command.headOption match {
      case Some("python") ⇒ (Some(PythonLanguage()), command.lift(1).getOrElse(""), indexArgs(command.drop(2), Seq()).map { case (k, v, i) ⇒ toVariableElement(k, v, i, CareTaskType()) })
      case Some("R")      ⇒ (Some(RLanguage()), "", rParsing(command.drop(1), CareTaskType()))
      case _              ⇒ (None, command.head, command.drop(1))
    }

    //Parse the arguments and return the LaunchingCommand

    Some(
      LaunchingCommand(
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
    case Some(s: String) ⇒ s matches ("""(.*)[.]([^.]{1,3})""")
    case _               ⇒ false
  }

  private def indexArgs(args: Seq[String], indexed: Seq[(Option[String], Option[String], Int)]): Seq[(Option[String], Option[String], Int)] = {
    if (args.isEmpty) indexed
    else {
      val head = args.head
      val nextIndex = indexed.lastOption.map { _._3 }.getOrElse(-1) + 1
      if (head.startsWith("-")) {
        indexArgs(args.drop(2), indexed :+ (
          Some(head), {
            if (args.size >= 2) {
              if (!args(1).startsWith("-")) Some(args(1)) else None
            }
            else None
          }, nextIndex)
        )
      }
      else indexArgs(args.drop(1), indexed :+ (None, Some(head), nextIndex))
    }
  }

  private def toVariableElement(key: Option[String], value: Option[String], index: Int, taskType: TaskType): VariableElement = {
    val isFile = isFileString(value)
    VariableElement(index, ProtoTypePair(key match {
      case Some(k: String) ⇒ k
      case _ ⇒ if (isFile) {
        value match {
          case Some(v: String) ⇒ v.split('.').dropRight(1).mkString(".")
          case _               ⇒ "i" + index
        }
      }
      else "i" + index
    },
      value match {
        case Some(a: String) ⇒ if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE
        case _               ⇒ ProtoTYPE.DOUBLE
      },
      if (isFile) value else None
    ),
      taskType
    )
  }

  private def rParsing(args: Seq[String], taskType: TaskType): Seq[CommandElement] = {
    val indexed = indexArgs(args, Seq())

    val (others, toBeParsed) = indexed.partition { p ⇒
      Seq("--slave", "--args").contains(p._1.getOrElse(""))
    }

    val (f, rest) = toBeParsed.partition { p ⇒ p._1 == Some("-f") }

    rest.map { r ⇒
      val (k, v, i) = r
      toVariableElement(k, v, i, taskType)
    } ++ others.map { case (k, _, i) ⇒ k.map { kk ⇒ StaticElement(i, kk) } }.flatten ++ f.map { x ⇒ StaticElement(x._3, "-f " + x._2.getOrElse("")) }
  }

  private def netlogoParsing(safePath: SafePath): LaunchingCommand = {
    val netlogoCoreObject = ServerFactories.coreObject("NetLogo")
    LaunchingCommand(None, "")
  }

}
