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

import org.openmole.core.batch.storage._
import fr.iscpif.gridscale.storage.SRMStorage
import java.net.URI
import java.io.File

object GliteStorageService {

  def emptyRoot(s: SRMStorage) =
    new SRMStorage {
      val host: String = s.host
      val port: Int = s.port
      val basePath: String = ""
    }

  def apply(s: SRMStorage, _environment: GliteEnvironment, caCertDir: File) = new PersistentStorageService {
    val storage = emptyRoot(s)
    val url = new URI("srm", null, s.host, s.port, null, null, null)
    val remoteStorage = new RemoteGliteStorage(s.host, s.port, caCertDir)
    val environment = _environment
    val root = s.basePath
    val connections = _environment.threadsBySE
    def authentication = environment.authentication._1
  }

}
