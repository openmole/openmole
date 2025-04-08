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

package org.openmole.tool.hash

import scala.util.hashing.MurmurHash3
import java.io.{File, FileInputStream, InputStream}
import java.security.MessageDigest
import org.openmole.tool.stream.*
import org.openmole.tool.file.*

object Hash:
  val HEX_CHAR_TABLE = List('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f').map { _.toByte }.toArray

  def hexString(raw: Array[Byte]): String =
    val hex = new Array[Byte](2 * raw.length)
    var index = 0

    for (b <- raw)
    do
      val v = b & 0xFF
      hex(index) = HEX_CHAR_TABLE(v >>> 4)
      index += 1
      hex(index) = HEX_CHAR_TABLE(v & 0xF)
      index += 1

    new String(hex, "ASCII")


  def string(s: String, hashType: HashType = HashType.default) = computeHash(new StringInputStream(s), hashType)

  def file(file: File, hashType: HashType = HashType.default): Hash =
    val is = new FileInputStream(file)
    try computeHash(is, hashType)
    finally is.close

  def computeHash(is: InputStream, hashType: HashType): Hash =
    val buffer = new Array[Byte](DefaultBufferSize)

    def digestAll(is: InputStream, digest: MessageDigest) =
      Iterator.continually(is.read(buffer)).takeWhile(_ != -1).foreach:
        count => digest.update(buffer, 0, count)
      Hash(digest.digest)

    def blakeHash(is: InputStream) =
      import org.apache.commons.codec.digest.*
      val hasher = Blake3.initHash()
      try
        var bytesRead = 0
        while
          bytesRead = is.read(buffer)
          bytesRead != -1
        do hasher.update(buffer, 0, bytesRead)
      finally is.close()

      Hash(hasher.doFinalize(32))

    hashType match
      case HashType.SHA1 => digestAll(is, MessageDigest.getInstance("SHA-1"))
      case HashType.SHA256 => digestAll(is, MessageDigest.getInstance("SHA-256"))
      case HashType.Blake3 => blakeHash(is)



  implicit val ordering: Ordering[Hash] = Ordering.by[Hash, String](_.toString)

case class Hash(content: Array[Byte]):
  def ==(hash: String) = this.toString == hash
  def !=(hash: String) = !this.==(hash)
  def equals(hash: String) = this == hash
  override def toString: String = Hash.hexString(content)
  override def hashCode: Int = MurmurHash3.arrayHash(content)
  override def equals(obj: Any): Boolean =
    if (obj == null) return false
    if (getClass != obj.asInstanceOf[AnyRef].getClass) return false
    val other = obj.asInstanceOf[Hash]
    content sameElements other.content

