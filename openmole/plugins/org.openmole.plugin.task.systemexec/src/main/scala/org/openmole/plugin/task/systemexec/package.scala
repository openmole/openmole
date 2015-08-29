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
import org.openmole.core.workflow.tools.VariableExpansion

package systemexec {
  trait SystemExecPackage {

    /**
     * Command line representation
     *
     * @param command the actual command line to be executed
     * @param isRemote whether the command is passed as a resource to the task or is present on the remote environment
     */
    case class Command(command: String, isRemote: Boolean)

    /**
     * Sequence of commands for a particular OS
     *
     * @param os target Operating System
     * @param parts Sequence of commands to be executed
     * @see Command
     */
    case class OSCommands(os: OS, parts: Command*) {
      @transient lazy val expanded = parts.map(c ⇒ (VariableExpansion(c.command), c.isRemote))
    }

    /** Make commands non-remote by default */
    implicit def stringToCommand(s: String) = Command(s, false)
    /** A single command can be a sequence  */
    implicit def stringToCommands(s: String) = OSCommands(OS(), s)
    /** A sequence of command lines is considered local (non-remote) by default */
    implicit def seqOfStringToCommands(s: Seq[String]): OSCommands = OSCommands(OS(), s.map(s ⇒ Command(s, false)): _*)

    /** Make a command line remote from the DSL */
    def remote(s: String) = Command(s, true)

    lazy val errorOnReturnCode = set[{ def setErrorOnReturnValue(b: Boolean) }]

    lazy val returnValue = set[{ def setReturnValue(v: Option[Prototype[Int]]) }]

    lazy val stdOut = set[{ def setStdOut(v: Option[Prototype[String]]) }]

    lazy val stdErr = set[{ def setStdErr(v: Option[Prototype[String]]) }]

    lazy val commands = add[{ def addCommand(os: OS, cmd: OSCommands*) }]

    lazy val environmentVariable =
      new {
        def +=(prototype: Prototype[_], variable: Option[String] = None) =
          (_: SystemExecTaskBuilder).addEnvironmentVariable(prototype, variable)
      }

    lazy val workDirectory = set[{ def setWorkDirectory(s: Option[String]) }]
  }
}

package object systemexec extends external.ExternalPackage with SystemExecPackage
