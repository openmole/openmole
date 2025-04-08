/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

import java.io.{ File, FileInputStream, InputStream }
import java.security.MessageDigest
import org.openmole.tool.stream._
import org.openmole.tool.file._

package object hash:

  object HashType:
    inline def default = HashType.SHA1

  enum HashType:
    case SHA1, SHA256

  implicit class StringHashDecorator(s: String):
    def hash(hashType: HashType = HashType.default) = hashString(s, hashType)

  implicit class FileHashServiceDecorator(file: File):
    def hash(hashType: HashType = HashType.default) = hashFile(file, hashType)

  implicit class InputStreamHashServiceDecorator(is: InputStream):
    def hash(hashType: HashType = HashType.default) = computeHash(is, hashType)

  def hashString(s: String, hashType: HashType = HashType.default) = computeHash(new StringInputStream(s), hashType)

  def hashFile(file: File, hashType: HashType = HashType.default): Hash =
    val is = new FileInputStream(file)
    try hash.computeHash(is, hashType)
    finally is.close

  def computeHash(is: InputStream, hashType: HashType): Hash =
    val buffer = new Array[Byte](DefaultBufferSize)
    val md =
      hashType match
        case HashType.SHA1   => MessageDigest.getInstance("SHA-1")
        case HashType.SHA256 => MessageDigest.getInstance("SHA-256")

    Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach:
      count => md.update(buffer, 0, count)

    Hash(md.digest)

