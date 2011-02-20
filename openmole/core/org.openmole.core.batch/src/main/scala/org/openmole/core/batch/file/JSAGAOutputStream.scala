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

package org.openmole.core.batch.file

import java.util.concurrent.TimeUnit
import java.io.OutputStream
import org.ogf.saga.error.TimeoutException
import org.ogf.saga.error.TimeoutException
import org.ogf.saga.file.FileOutputStream
import org.ogf.saga.task.TaskMode
import org.openmole.misc.workspace.Workspace

class JSAGAOutputStream(stream: FileOutputStream) extends OutputStream {

  override def write(b: Array[Byte], off: Int, len: Int) = stream.write(b, off, len)

  override def write(b: Array[Byte]) = stream.write(b)
    
  override def write(b: Int) = stream.write(b)

  override def flush = stream.flush()
    
  override def close() =  {
    val task = stream.close(TaskMode.ASYNC)
        
    try {
      task.get(Workspace.preferenceAsDurationInMs(URIFile.Timeout), TimeUnit.MILLISECONDS)
    } catch {
      case (e: TimeoutException) => 
        task.cancel(true)
        throw e
    } 
  }
}
