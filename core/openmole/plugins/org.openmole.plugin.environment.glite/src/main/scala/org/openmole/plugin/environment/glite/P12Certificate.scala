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

import fr.iscpif.gridscale.authentication._
import org.openmole.core.batch.authentication.CypheredPassword
import java.io.File

class P12Certificate(val certificate: File, val cypheredPassword: String) extends GliteAuthentication with CypheredPassword { a ⇒

  override def apply(
    serverURL: String,
    voName: String,
    proxyFile: File,
    lifeTime: Int,
    fqan: Option[String]) = {
    VOMSAuthentication.setCARepository(GliteAuthentication.CACertificatesDir)
    val (_serverURL, _voName, _proxyFile, _lifeTime, _fqan) = (serverURL, voName, proxyFile, lifeTime, fqan)
    new P12VOMSAuthentication {
      val certificate = a.certificate
      val serverURL = _serverURL
      val voName = _voName
      val proxyFile = _proxyFile
      val lifeTime = _lifeTime
      override val fqan = _fqan
    }.init(password)
  }
}
