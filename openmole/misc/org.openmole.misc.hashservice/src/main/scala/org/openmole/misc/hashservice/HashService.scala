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

package org.openmole.misc.hashservice

import gnu.crypto.hash.Sha160
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.ReaderRunnable

object HashService {

  implicit def fileHashServiceDecorator(file: File) = new {
    def hash = computeHash(file)
  }

  implicit def inputStreamHashServiceDecorator(is: InputStream) = new Object {
    def hash = computeHash(is)
    def hash(maxRead: Int, timeout: Long) = computeHash(is, maxRead, timeout)
  }

  def computeHash(file: File): SHA1Hash = {
    val is = new FileInputStream(file)
    try computeHash(is)
    finally is.close
  }

  def computeHash(is: InputStream): SHA1Hash = {

    val buffer = new Array[Byte](FileUtil.DefaultBufferSize)
    val md = new Sha160
    Stream.continually(is.read(buffer)).takeWhile(_ != -1).foreach {
      count ⇒ md.update(buffer, 0, count)
    }

    new SHA1Hash(md.digest)
  }

  def computeHash(is: InputStream, maxRead: Int, timeout: Long): SHA1Hash = {
    val buffer = new Array[Byte](maxRead)
    val md = new Sha160

    val executor = Executors.newSingleThreadExecutor
    val reader = new ReaderRunnable(buffer, is, maxRead)

    Iterator.continually({
      val f = executor.submit(reader)

      try {
        f.get(timeout, TimeUnit.MILLISECONDS)
      } catch {
        case (e: TimeoutException) ⇒
          f.cancel(true)
          throw new IOException("Timout on reading, read was longer than " + timeout, e)
      }
    }).takeWhile(_ != -1).foreach {
      count ⇒ md.update(buffer, 0, count)
    }

    new SHA1Hash(md.digest)
  }
}
