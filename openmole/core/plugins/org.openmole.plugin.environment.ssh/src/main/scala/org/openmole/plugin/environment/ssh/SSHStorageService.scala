/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.ssh

import org.openmole.core.batch.storage.StorageService
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.plugin.environment.gridscale.LocalStorage
import java.net.URI
import org.openmole.misc.workspace.Workspace

trait SSHStorageService extends StorageService with SSHService { ss ⇒

  def createStorage(_root: String) = new fr.iscpif.gridscale.ssh.SSHStorage {
    val host = ss.host
    override val port = ss.port
    val user = ss.user
    val root = _root
    override def timeout = Workspace.preferenceAsDuration(SSHService.timeout).toMilliSeconds.toInt
  }

  lazy val storage = createStorage(root)

  def home = createStorage("/").home(authentication)

  lazy val remoteStorage = new LocalStorage(root)

}
