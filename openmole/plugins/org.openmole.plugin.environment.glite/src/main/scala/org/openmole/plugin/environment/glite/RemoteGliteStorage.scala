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

package org.openmole.plugin.environment.glite

import org.openmole.core.batch.storage.Storage
import org.openmole.misc.exception._
import fr.iscpif.gridscale.storage.SRMStorage
import fr.iscpif.gridscale.authentication.ProxyFileAuthentication
import java.io.File

class RemoteGliteStorage(val storage: SRMStorage) extends Storage {
  def root = ""

  @transient lazy val authentication: storage.A = new ProxyFileAuthentication {
    def proxy = {
      val path = if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) System.getenv.get("X509_USER_PROXY")
      else throw new InternalProcessingError("The X509_USER_PROXY environment variable is not defined or point to an inexisting file.")
      new File(path)
    }
  }.init
}
