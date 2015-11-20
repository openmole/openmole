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

object CodeArgParsing {

  implicit def stringsToCommandArguments(seqS: Seq[String]): Seq[CommandArgument] = defaultParseArgs(seqS, Seq())

  def apply(command: Seq[String]) = {
    val (language, codeName, args: Seq[CommandArgument]) = command.headOption match {
      case Some("python") ⇒ (Some(PythonLanguage()), command.lift(1).getOrElse(""), command.drop(2))
      case Some("R")      ⇒ (Some(RLanguage()), command.lift(1).getOrElse(""), rParsing(command.drop(1), Seq()))
      case _              ⇒ (None, command.head, command.drop(1))
    }

    //Parse the arguments and return the LaunchingCommand
    lazy val prototypePairs = args.filter {
      _.value.isDefined
    }.zipWithIndex map {
      case (arg, index) ⇒
        val isFile = arg.isFile
        (arg.value, ProtoTypePair(arg.key match {
          case Some(k: String) ⇒ k
          case _ ⇒ if (isFile) {
            arg.value match {
              case Some(v: String) ⇒ v.split(".").dropRight(1).mkString(".")
              case _               ⇒ "i" + index
            }
          }
          else "i" + index
        },
          arg.value match {
            case Some(a: String) ⇒ if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE
            case _               ⇒ ProtoTYPE.DOUBLE
          },
          if (isFile) arg.value else None
        )
        )
    }

    Some(LaunchingCommand(
      language,
      codeName,
      switchArgumentWithPrototype(language, command.mkString(" "), prototypePairs), prototypePairs.map {
        _._2
      }))
  }

  private def switchArgumentWithPrototype(language: Option[Language], commandLine: String, prototypePairs: Seq[(Option[String], ProtoTypePair)]) = {
    val (pre, post) = language.map { l ⇒ (l.taskType.preVariable, l.taskType.postVariable) }.getOrElse(("", ""))

    def replaceInCommand(command: String, prototypePairs: Seq[(Option[String], ProtoTypePair)]): String = {
      if (prototypePairs.isEmpty) command
      else {
        val first = prototypePairs.head
        first._1 match {
          case Some(s: String) ⇒ replaceInCommand(command.replace(s, pre + first._2.name + post), prototypePairs.tail)
          case _               ⇒ replaceInCommand(command, prototypePairs.tail)
        }
      }
    }

    replaceInCommand(commandLine, prototypePairs)
  }

  private def defaultParseArgs(toBeParsed: Seq[String], parsed: Seq[CommandArgument]): Seq[CommandArgument] = {
    if (toBeParsed.isEmpty) parsed
    else {
      val head = toBeParsed.head
      if (head.startsWith("-")) {
        defaultParseArgs(toBeParsed.drop(2), parsed :+ CommandArgument(
          Some(head),
          if (toBeParsed.size >= 2) {
            if (!toBeParsed(1).startsWith("-")) Some(toBeParsed(1)) else None
          }
          else None))
      }
      else defaultParseArgs(toBeParsed.drop(1), parsed :+ CommandArgument(None, Some(head)))
    }
  }

  private def rParsing(toBeParsed: Seq[String], parsed: Seq[CommandArgument]): Seq[CommandArgument] = {
    val filteredToBeParsed = toBeParsed.filterNot { p ⇒
      Seq("--slave", "--args").contains(p)
    }

    val (st, end) = filteredToBeParsed.splitAt(filteredToBeParsed.indexOf("-f"))
    defaultParseArgs(st ++ end.drop(2), Seq())
  }

}
