/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.core.batch.storage

import org.openmole.core.batch.control._
import org.openmole.core.batch.replication.ReplicaCatalog
import scala.slick.driver.H2Driver.simple._

trait VolatileStorageService extends StorageService { this: Storage ⇒

  override protected def createBasePath(implicit token: AccessToken) = {
    ReplicaCatalog.withSession { implicit c ⇒
      ReplicaCatalog.onStorage(this).delete
    }
    val path = super.createBasePath
    rmDir(path)
    makeDir(path)
    path
  }

  def persistentDir(implicit token: AccessToken, session: Session) = baseDir(token)
  def tmpDir(implicit token: AccessToken) = baseDir(token)
}
