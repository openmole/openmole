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

import org.openmole.core.batch.storage.SimpleStorage
import org.openmole.misc.workspace._
import org.openmole.misc.exception._
import fr.iscpif.gridscale.storage.SRMStorage
import fr.iscpif.gridscale.authentication.ProxyFileAuthentication
import fr.iscpif.gridscale.authentication.VOMSAuthentication
import java.io.File

class RemoteGliteStorage(val host: String, val port: Int, certificateDir: File) extends SimpleStorage { s ⇒
  def root = ""

  @transient lazy val storage =
    new SRMStorage {
      val host: String = s.host
      val port: Int = s.port
      val basePath: String = ""
      override val timeout = Workspace.preferenceAsDuration(GliteEnvironment.RemoteTimeout).toSeconds
    }

  def authentication: SRMStorage#A = new ProxyFileAuthentication {
    def proxy = {
      //val X509_CERT_DIR = System.getenv("X509_CERT_DIR")

      //      val certificateDir =
      //        if (X509_CERT_DIR != null && new File(X509_CERT_DIR).exists) new File(X509_CERT_DIR)
      //        else throw new InternalProcessingError("X509_CERT_DIR environment variable not found or directory doesn't exists.")

      VOMSAuthentication.setCARepository(certificateDir)

      val path = if (System.getenv.containsKey("X509_USER_PROXY") && new File(System.getenv.get("X509_USER_PROXY")).exists) System.getenv.get("X509_USER_PROXY")
      else throw new InternalProcessingError("The X509_USER_PROXY environment variable is not defined or point to an inexisting file.")
      new File(path)
    }
  }
}
