package org.openmole.plugin.task.systemexec

/*
 * Copyright (C) 2021 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import org.scalatest.*
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

class SystemExecTaskSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test._

  "SystemExecTask" should "be instantiated" in {
    val out = Val[String]

    SystemExecTask("ls")

    val cmd: Seq[Command] = Seq("ls", "ls /tmp")
    val outOpt: OptionalArgument[Val[String]] = out

    SystemExecTask(
      command = Seq("ls", "ls /tmp"),
      stdOut = out
    )
  }
}
