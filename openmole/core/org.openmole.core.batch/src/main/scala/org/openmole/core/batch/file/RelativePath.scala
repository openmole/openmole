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
  
  implicit def stringDecorator(path: String) = new {
    def cacheUnziped = toGZURIFile.cache
    def cache = toURIFile.cache
    def cacheUnziped(token: AccessToken) = toGZURIFile.cache(token)
    def cache(token: AccessToken) = toURIFile.cache(token)
    def toURIFile = new URIFile(toURI)
    def toGZURIFile = new GZURIFile(toURI)
    def toURI = root.resolve(path)
    def toStringURI = toURI.toString
  }
}
