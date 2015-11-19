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
      case Some("R")      ⇒ (Some(RLanguage()), command.lift(1).getOrElse(""), rParsing(command.drop(2), Seq()))
      case _              ⇒ (None, command.head, command.drop(1))
    }

    //Parse the arguments and return the LaunchingCommand
    Some(LaunchingCommand(language, codeName, args))
  }

  private def defaultParseArgs(toBeParsed: Seq[String], parsed: Seq[CommandArgument]): Seq[CommandArgument] = {
    if (toBeParsed.isEmpty) parsed
    else {
      if (toBeParsed.head.startsWith("-"))
        defaultParseArgs(toBeParsed.drop(2), parsed :+ CommandArgument(
          Some(toBeParsed(0)),
          if (toBeParsed.size >= 2) {
            if (!toBeParsed(1).startsWith("-")) Some(toBeParsed(1)) else None
          }
          else None))
      else defaultParseArgs(toBeParsed.drop(1), parsed :+ CommandArgument(None, Some(toBeParsed(0))))
    }
  }

  private def rParsing(toBeParsed: Seq[String], parsed: Seq[CommandArgument]): Seq[CommandArgument] = defaultParseArgs(toBeParsed, Seq())

}
