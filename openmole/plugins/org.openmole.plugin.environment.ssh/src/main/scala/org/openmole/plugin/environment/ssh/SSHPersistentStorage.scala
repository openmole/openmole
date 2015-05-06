/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.plugin.environment.ssh

import java.net.URI

import org.openmole.core.batch.control.{ UnlimitedAccess, LimitedAccess }
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.storage._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.{ LocalStorage, LogicalLinkStorage }

trait SSHPersistentStorage <: BatchEnvironment with SSHAccess { env ⇒

  type SS = PersistentStorageService

  def sharedDirectory: Option[String]

  trait StorageRoot <: Storage {
    def home: String
    lazy val root = sharedDirectory match {
      case Some(p) ⇒ p
      case None    ⇒ child(home, ".openmole/.tmp/ssh/")
    }
  }

  def storageSharedLocally: Boolean

  lazy val storage =
    storageSharedLocally match {
      case true ⇒
        new PersistentStorageService with LogicalLinkStorage with StorageRoot with UnlimitedAccess {
          lazy val remoteStorage: RemoteStorage = new RemoteLogicalLinkStorage(root)
          val url = new URI("file", env.user, "localhost", -1, sharedDirectory.orNull, null, null)
          val id: String = url.toString
          val environment = env
        }
      case false ⇒
        new PersistentStorageService with SSHStorageService with StorageRoot with LimitedAccess with ThisHost {
          def nbTokens = maxConnections
          val environment = env
          val id = new URI("ssh", env.user, env.host, env.port, sharedDirectory.orNull, null, null).toString
        }
    }

  def allStorages = List(storage)

}
