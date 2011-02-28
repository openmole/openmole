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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.hashservice

import gnu.crypto.hash.Sha160
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.ReaderRunnable
import org.openmole.misc.executorservice.ExecutorService
import org.openmole.misc.executorservice.ExecutorType

object HashService {

  def computeHash(file: File): SHA1Hash = {
    val is = new FileInputStream(file)
    try {
      return computeHash(is)
    } finally {
      is.close
    }
  }

  def computeHash(is: InputStream): SHA1Hash = {
        
    val buffer = new Array[Byte](FileUtil.DefaultBufferSize)
    val md = new Sha160
    Stream.continually(is.read(buffer)).takeWhile(_ != -1).foreach{ 
      count => md.update(buffer, 0, count)
    }
    
    new SHA1Hash(md.digest)
  }

  def computeHash(is: InputStream, maxRead: Int, timeout: Long): SHA1Hash = {
    val buffer = new Array[Byte](maxRead)
    val md = new Sha160

    val thread = ExecutorService.executorService(ExecutorType.OWN)
    val reader = new ReaderRunnable(buffer, is, maxRead)

    Stream.continually( {
        val f = thread.submit(reader)

        try {
          f.get(timeout, TimeUnit.MILLISECONDS)
        } catch {
          case (e: TimeoutException) =>
            f.cancel(true)
            throw new IOException("Timout on reading, read was longer than " + timeout, e)
        }
      }).takeWhile(_ != -1).foreach{ 
      count => md.update(buffer, 0, count)
    }
                    
    new SHA1Hash(md.digest)
  }
}
