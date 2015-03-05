/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.tools.io

import java.io.{ File, FileInputStream, InputStream }
import java.security.MessageDigest

import org.openmole.core.tools.service.Hash

object HashService {

  implicit class FileHashServiceDecorator(file: File) {
    def hash = computeHash(file)
  }

  implicit class InputStreamHashServiceDecorator(is: InputStream) {
    def hash = computeHash(is)
  }

  def computeHash(file: File): Hash = {
    val is = new FileInputStream(file)
    try computeHash(is)
    finally is.close
  }

  def computeHash(is: InputStream): Hash = {
    val buffer = new Array[Byte](FileUtil.DefaultBufferSize)
    val md = MessageDigest.getInstance("SHA1")
    Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach {
      count ⇒ md.update(buffer, 0, count)
    }
    Hash(md.digest)
  }
}
