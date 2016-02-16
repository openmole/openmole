/*
 *  Copyright (C) 2015 Jonathan Passerat-Palmbach
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.care

import java.io.File

import org.openmole.core.workflow.builder.CanBuildTask
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.task.systemexec._
import org.openmole.plugin.task.systemexec
import org.openmole.core.workflow.data._

// arguments to SystemExecTask not really matching the actual one -> set in toTask
abstract class CARETaskBuilder(archiveLocation: File,
                               command: systemexec.Command,
                               archiveWorkDirectory: Option[String])
    extends SystemExecTaskBuilder(Seq.empty: _*) { builder ⇒

  // TODO handle shallow copies (bind to archive)
  // one option would be to replace call line by bind + call line
  //

  // FIXME refactor
  implicit object canBuildTask2 extends CanBuildTask[CARETask] {
    override def toTask: CARETask = new CARETask(
      archiveLocation, command, archiveWorkDirectory,
      errorOnReturnValue, returnValue, stdOut, stdErr, variables.toList) with builder.Built {

      override val outputs: PrototypeSet = builder.outputs + List(stdOut, stdErr, returnValue).flatten
    }
  }

}
