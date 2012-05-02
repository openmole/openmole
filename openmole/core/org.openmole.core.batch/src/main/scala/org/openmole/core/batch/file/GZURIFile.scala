/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.batch.file

import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.openmole.core.batch.control.AccessToken

class GZURIFile(location: String) extends URIFile(location) {

  def this(file: IURIFile) = this(file.location)
  def this(uri: URI) = this(uri.toString)

  override def openInputStream(token: AccessToken): InputStream = new GZIPInputStream(super.openInputStream(token))
  override def openOutputStream(token: AccessToken): OutputStream = new GZIPOutputStream(super.openOutputStream(token))
}
