/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.environment.desktopgrid

import org.openmole.core.batch.authentication.Authentication
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.environment.JobService
import org.openmole.core.batch.environment.VolatileStorage
import java.util.concurrent.Executors

import org.openmole.core.batch.control.ServiceDescription
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.sftpserver.SFTPServer
import org.openmole.misc.tools.io.FileUtil._
import java.io.File
import collection.JavaConversions._

object DesktopEnvironment {
  val timeStempsDirName = "timeStemps"
  val jobsDirName = "jobs"
  val resultsDirName = "results"
  val timeStempSeparator = '@'
}

class DesktopGridEnvironment(
    port: Int,
    login: String, password: String,
    val runtimeMemory: Int = BatchEnvironment.defaultRuntimeMemory) extends BatchEnvironment {

  val path = Workspace.newDir
  new SFTPServer(path, login, password, port)

  val description = new ServiceDescription(login + "@localhost:" + port)

  @transient lazy val batchStorage = new VolatileStorage(this, path.toURI, Int.MaxValue) {
    override lazy val description = DesktopGridEnvironment.this.description
  }

  @transient override lazy val allStorages = List(batchStorage)
  @transient override lazy val allJobServices = List(new DesktopGridJobService(this, DesktopGridEnvironment.this.description))

  override def authentication = DesktopGridAuthentication
}
