/**
 * Created by Romain Reuillon on 28/01/16.
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
 *
 */
package org.openmole.tool.stream

import java.io.{ ByteArrayOutputStream, InputStream }
import java.util.zip.GZIPOutputStream

class GZipedInputStream(is: InputStream) extends InputStream {

  var end = false
  var buffer = Array.empty[Byte]
  var cur = 0

  val byteArrayOutputStream = new ByteArrayOutputStream(1024)
  val gzip = new GZIPOutputStream(byteArrayOutputStream)

  bufferizeByteArrayStream()

  override def read(): Int = synchronized {
    while (bufferEmpty && !end) readFromInput()
    if (!bufferEmpty) readBuffer() & 0xFF
    else -1
  }

  override def close = is.close

  private def readFromInput() = {
    val localBuffer: Array[Byte] = new Array[Byte](1024)
    val r = is.read(localBuffer)
    if (r != -1) {
      gzip.write(localBuffer, 0, r)
      bufferizeByteArrayStream()
    }
    else {
      gzip.finish()
      bufferizeByteArrayStream()
      end = true
    }
  }

  private def bufferizeByteArrayStream() = {
    gzip.flush()
    if (byteArrayOutputStream.size() > 0) {
      setBuffer(byteArrayOutputStream.toByteArray)
      byteArrayOutputStream.reset()
    }
  }

  private def readBuffer() = {
    val res = buffer(cur)
    cur += 1
    res
  }

  private def bufferEmpty = cur >= buffer.size

  private def setBuffer(array: Array[Byte]) = {
    buffer = byteArrayOutputStream.toByteArray
    cur = 0
  }
}
