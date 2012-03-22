/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.core.batch.file

import java.net.URI
import org.openmole.core.batch.control.AccessToken

class RelativePath(root: URI) {

  def this(root: String) = this(new URI(root))

  def cacheUnziped(path: String) = toGZURIFile(path).cache
  def cache(path: String) = toURIFile(path).cache
  def cacheUnziped(path: String, token: AccessToken) = toGZURIFile(path).cache(token)
  def cache(path: String, token: AccessToken) = toURIFile(path).cache(token)
  def toURIFile(path: String) = new URIFile(toURI(path))
  def toGZURIFile(path: String) = new GZURIFile(toURI(path))
  def toURI(path: String) = root.resolve(path)
  def toStringURI(path: String) = toURI(path).toString
}
