package org.openmole.gui.plugin.task.systemexec.ext

/*
 * Copyright (C) 19/10/2014 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.ext.data.{InOutput, TaskData}

case class SystemExecTaskData(inputs: Seq[InOutput] = Seq(),
                              outputs: Seq[InOutput] = Seq() // val command: String = "",
                              // val directory: String = "",
                              // val errorOnReturnCode: Boolean = true,
                              // val returnValue: Option[PrototypeData[Int]] = None,
                              // val output: Option[PrototypeData[String]] = None,
                              // val error: Option[PrototypeData[String]] = None
                               ) extends TaskData