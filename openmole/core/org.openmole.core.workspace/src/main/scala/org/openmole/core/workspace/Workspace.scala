/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workspace

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import java.util.logging.Logger
import org.jasypt.util.text.*
import org.openmole.core.event.{Event, EventDispatcher}
import org.openmole.core.exception.{InternalProcessingError, UserBadDataError}
import org.openmole.tool.types.*
import org.openmole.tool.crypto.Certificate
import org.openmole.tool.file.*
import org.openmole.core.tools.service.*
import org.openmole.core.workspace.Workspace.{fixedDir, fixedPostfix, fixedPrefix}
import org.openmole.tool.network.fixHostName
import org.openmole.tool.random
import org.openmole.tool.random.Random
import org.openmole.tool.random.Random.*

import scala.concurrent.stm.{Ref, atomic}
import squants.information.*

import java.io.FileOutputStream

object Workspace:

  case object PasswordRequired extends Event[Workspace]

  def tmpLocation = "tmp"
  def tmpLock = ".tmp.lock"
  def persistentLocation = "persistent"
  def userLocation = "user"

  def logLocation = "openmole.log.gz"

  def fixedPrefix = "file"
  def fixedPostfix = ".bin"
  def fixedDir = "dir"

  case class Lock(os: FileOutputStream)

  // Workspace should be cleaned after use
  def apply(location: File): Workspace =
    val tmpDir = location / tmpLocation /> UUID.randomUUID.toString
    val persistentDir = location /> persistentLocation
    val userDir = location /> userLocation

    val os = (tmpDir / tmpLock).fileOutputStream
    os.getChannel.tryLock

    new Workspace(location, tmpDir, persistentDir, userDir , os)

  def clean(ws: Workspace) =
    ws.os.close()

    (ws.location / tmpLocation).listFilesSafe.foreach: f =>
      val lockFile = f / tmpLock
      if !lockFile.exists() || lockFile.withFileOutputStream(_.getChannel.tryLock() != null)
      then f.recursiveDelete

    ws.tmpDirectory.recursiveDelete

case class Workspace(location: File, tmpDirectory: File, persistentDir: File, userDir: File, os: FileOutputStream)

