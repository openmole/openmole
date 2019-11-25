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

import org.jasypt.util.text._
import org.openmole.core.event.{ Event, EventDispatcher }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.tool.types._
import org.openmole.tool.crypto.Certificate
import org.openmole.tool.file._
import org.openmole.core.tools.service._
import org.openmole.core.workspace.Workspace.{ fixedDir, fixedPostfix, fixedPrefix }
import org.openmole.tool.network.fixHostName
import org.openmole.tool.random
import org.openmole.tool.random.Random
import org.openmole.tool.random.Random._

import scala.concurrent.stm.{ Ref, atomic }

object Workspace {

  case object PasswordRequired extends Event[Workspace]

  def tmpLocation = ".tmp"
  def persistentLocation = "persistent"

  def fixedPrefix = "file"
  def fixedPostfix = ".bin"
  def fixedDir = "dir"

  lazy val defaultLocation = new File(System.getProperty("user.home"), s".openmole/${fixHostName}/")

  // Workspace should be cleaned manualy
  def apply(location: File): Workspace = {
    val tmpDir = location / tmpLocation /> UUID.randomUUID.toString
    val persistentDir = location /> persistentLocation
    new Workspace(location, tmpDir, persistentDir)
  }

  def clean(ws: Workspace) = ws.tmpDirectory.recursiveDelete
}

class Workspace(val location: File, val tmpDirectory: File, val persistentDir: File)

