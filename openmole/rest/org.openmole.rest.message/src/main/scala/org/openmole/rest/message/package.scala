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
package org.openmole.rest.message

import java.io.{ PrintWriter, StringWriter }
import io.circe.*

implicit val derivationDefault: io.circe.derivation.Configuration =
  io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type")

object Error:
  def apply(e: Throwable): Error =
    val sw = new StringWriter()
    e.printStackTrace(new PrintWriter(sw))
    Error(e.getMessage, Some(sw.toString))

case class Error(message: String, stackTrace: Option[String] = None, level: Option[String] = None) derives derivation.ConfiguredCodec

case class ExecutionId(id: String) derives derivation.ConfiguredCodec
case class Output(output: String) derives derivation.ConfiguredCodec

object ExecutionState:
  type ExecutionState = String
  val running: ExecutionState = "running"
  val finished: ExecutionState = "finished"
  val failed: ExecutionState = "failed"

case class Failed(error: Error, state: ExecutionState.ExecutionState = ExecutionState.failed) derives derivation.ConfiguredCodec
case class Running(ready: Long, running: Long, completed: Long, capsules: Vector[(String, CapsuleState)], environments: Seq[EnvironmentStatus], state: ExecutionState.ExecutionState = ExecutionState.running) derives derivation.ConfiguredCodec
case class Finished(state: ExecutionState.ExecutionState = ExecutionState.finished) derives derivation.ConfiguredCodec

case class EnvironmentStatus(name: Option[String], submitted: Long, running: Long, done: Long, failed: Long, errors: Seq[Error]) derives derivation.ConfiguredCodec
case class CapsuleState(ready: Long, running: Long, completed: Long) derives derivation.ConfiguredCodec

object FileType:
  type FileType = String
  val directory: FileType = "directory"
  val file: FileType = "file"

case class FileProperty(size: Long, modified: Long, `type`: FileType.FileType = FileType.file) derives derivation.ConfiguredCodec
case class DirectoryProperty(entries: Vector[DirectoryEntry], modified: Long, `type`: FileType.FileType = FileType.directory) derives derivation.ConfiguredCodec
case class DirectoryEntry(name: String, size: Option[Long], modified: Long, `type`: FileType.FileType) derives derivation.ConfiguredCodec
