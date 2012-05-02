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
import java.util.concurrent.TimeUnit

import org.ogf.saga.error.TimeoutException
import org.ogf.saga.file.FileInputStream
import org.ogf.saga.task.TaskMode
import org.openmole.misc.workspace.Workspace

class JSAGAInputStream(stream: FileInputStream) extends InputStream {

  override def skip(n: Long): Long = stream.skip(n)

  override def reset = synchronized { stream.reset() }

  override def read(b: Array[Byte], off: Int, len: Int): Int = stream.read(b, off, len)

  override def read(b: Array[Byte]): Int = stream.read(b);

  override def read: Int = stream.read()

  override def markSupported: Boolean = stream.markSupported()

  override def mark(readlimit: Int) = synchronized { stream.mark(readlimit) }

  override def close = {
    val task = stream.close(TaskMode.ASYNC)

    try {
      task.get(Workspace.preferenceAsDurationInMs(URIFile.Timeout), TimeUnit.MILLISECONDS)
    } catch {
      case (e: TimeoutException) ⇒ task.cancel(true); throw e;
    }
  }

  override def available(): Int = stream.available
}
