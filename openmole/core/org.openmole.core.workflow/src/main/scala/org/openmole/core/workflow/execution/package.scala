/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.core.workflow.execution

import org.jasypt.encryption.pbe.config.EnvironmentPBEConfig

import java.io.PrintStream
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.tool.cache.KeyValueCache


def display(stream: PrintStream, label: String, content: String) =
  if content.nonEmpty
  then
    stream.synchronized:
      val fullLength = 40
      val dashes = fullLength - label.length / 2
      val header = ("-" * dashes) + label + ("-" * (dashes - (label.length % 2)))
      val footer = "-" * header.length
      stream.println(header)
      stream.print(content)
      stream.println(footer)


type ExecutionState = Byte


object RuntimeLog:
  @transient lazy val localHost: RuntimeInfo =
    import org.openmole.tool.network.LocalHostName
    LocalHostName.localHostName.getOrElse("fake:" + java.util.UUID.randomUUID().toString)

  object RuntimeInfo:
    extension (r: RuntimeInfo)
      def hostName: String = r

  opaque type RuntimeInfo = String

case class RuntimeLog(beginTime: Long, executionBeginTime: Long, executionEndTime: Long, endTime: Long)

object RuntimeSetting:
  def apply(memoryOverlay: Boolean = false) =
    new RuntimeSetting(memoryOverlay)

case class RuntimeSetting(memoryOverlay: Boolean)

