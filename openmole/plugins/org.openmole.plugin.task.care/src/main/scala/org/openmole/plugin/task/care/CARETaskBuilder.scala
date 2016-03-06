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

import org.openmole.plugin.task.external.ExternalTaskBuilder
import org.openmole.plugin.task.systemexec._
import org.openmole.plugin.task.systemexec
import org.openmole.core.workflow.data._

import scala.collection.mutable.ListBuffer

class CARETaskBuilder(archiveLocation: File, command: systemexec.Command)
    extends ExternalTaskBuilder
    with ReturnValue
    with ErrorOnReturnCode
    with StdOutErr
    with EnvironmentVariables
    with WorkDirectory { builder ⇒

  val hostFiles = ListBuffer[(String, Option[String])]()

  def addHostFile(hostFile: String, binding: Option[String] = None): this.type = {
    hostFiles.append(hostFile → binding)
    this
  }

  override def toTask: CARETask = new CARETask(
    archiveLocation,
    command,
    workDirectory,
    errorOnReturnValue,
    returnValue,
    stdOut,
    stdErr,
    environmentVariables.toList,
    hostFiles.toList
  ) with builder.Built {
    override val outputs: PrototypeSet = builder.outputs + List(stdOut, stdErr, returnValue).flatten
  }

}
