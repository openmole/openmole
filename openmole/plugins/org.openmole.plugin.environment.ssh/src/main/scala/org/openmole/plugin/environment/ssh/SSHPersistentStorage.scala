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

import java.io.File
import java.net.URI

import org.openmole.core.batch.control.{ LimitedAccess, UnlimitedAccess, UsageControl }
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.storage._
import org.openmole.core.workspace.Workspace
import org.openmole.plugin.environment.gridscale.{ LocalStorage, LogicalLinkStorage }

trait SSHPersistentStorage <: BatchEnvironment with SSHAccess { st ⇒

  type SS = StorageService

  def user: String
  def host: String
  def port: Int

  def usageControl: UsageControl
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
        new StorageService with LogicalLinkStorage with StorageRoot {
          val usageControl = new UnlimitedAccess
          lazy val remoteStorage: RemoteStorage = new RemoteLogicalLinkStorage(root)
          val url = new URI("file", st.user, "localhost", -1, sharedDirectory.orNull, null, null)
          val id: String = url.toString
          val environment = st

          override def parent(path: String) =
            Option(new File(path).getCanonicalFile.getParentFile).map(_.getAbsolutePath)
        }
      case false ⇒
        new StorageService with SSHStorageService with StorageRoot with ThisHost {
          val usageControl = st.usageControl
          val environment = st
          val id = new URI("ssh", st.user, st.host, st.port, sharedDirectory.orNull, null, null).toString
        }
    }

}
