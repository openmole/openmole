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

import org.openmole.core.batch.control.LimitedAccess
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.batch.storage.PersistentStorageService

trait SSHPersistentStorage <: BatchEnvironment with SSHAccess { env ⇒

  type SS = PersistentStorageService with SSHStorageService

  def maxConnections: Int
  def path: Option[String]

  @transient lazy val storage = new PersistentStorageService with SSHStorageService with LimitedAccess with ThisHost {
    def nbTokens = maxConnections
    lazy val root = path match {
      case Some(p) ⇒ p
      case None    ⇒ child(home, ".openmole/.tmp/ssh/")
    }
    val environment = env
  }

  def allStorages = List(storage)

}
