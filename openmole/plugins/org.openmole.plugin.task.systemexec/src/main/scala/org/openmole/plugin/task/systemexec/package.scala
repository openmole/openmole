/*
 * Copyright (C) 2014 Romain Reuillon
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
 */

package org.openmole.plugin.task

import org.openmole.core.macros.Keyword._
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data.Prototype
import org.openmole.core.workflow.builder._

package object systemexec extends external.ExternalPackage {
  case class Commands(os: OS, parts: String*)

  implicit def stringToCommands(s: String) = Commands(OS(), s)
  implicit def seqOfStringToCommands(s: Seq[String]) = Commands(OS(), s: _*)

  lazy val errorOnReturnCode = set[{ def setErrorOnReturnValue(b: Boolean) }]

  lazy val returnValue = set[{ def setReturnValue(v: Option[Prototype[Int]]) }]

  lazy val stdOut = set[{ def setStdOut(v: Option[Prototype[String]]) }]

  lazy val stdErr = set[{ def setStdErr(v: Option[Prototype[String]]) }]

  lazy val commands = add[{ def addCommand(os: OS, cmd: String*) }]

  lazy val environmentVariable =
    new {
      def +=(prototype: Prototype[_], variable: Option[String] = None) =
        (_: SystemExecTaskBuilder).addEnvironmentVariable(prototype, variable)
    }

  lazy val workDirectory = set[{ def setWorkDirectory(s: Option[String]) }]
}
