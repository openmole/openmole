/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.runtime.daemon

import java.io.File
import org.openmole.core.batch.message.FileMessage
import org.openmole.core.batch.message.ReplicatedFile
import org.openmole.misc.tools.service.Logger
import scala.collection.mutable.HashMap
import org.openmole.misc.tools.io.FileUtil._

object FileCache extends Logger

import FileCache._

abstract class FileCache {

  def limit: Long
  
  val cached = new HashMap[String, (File, Long, Int)]
  
  def size = synchronized { cached.map {case(hash, (file, _, _)) => file.length.toLong}.sum }
  
  def cache(f: FileMessage, get: FileMessage => (File, String)) = synchronized {
    cached.get(f.hash) match {
      case Some((file, access, usage)) => 
        cached(f.hash) = (file, System.currentTimeMillis, usage + 1)
        file
      case None =>
        val (file, hash) = get(f)
        cached(f.hash) = (file, System.currentTimeMillis, 1)
        file
    }
  }
  
  def release(f: Iterable[FileMessage]) = synchronized {
    f.foreach(releaseOne)
    enforceLimit
  }
  
  
  def release(f: FileMessage) = synchronized {
    releaseOne(f)
    enforceLimit
  }
  
  private def releaseOne(f: FileMessage) = synchronized {
    cached.get(f.hash) match {
      case Some((file, access, usage)) =>
        cached(f.hash) = (file, System.currentTimeMillis, usage - 1)
      case None => logger.warning("Hash " + f.hash + " not in cache")
    }
  }
  
  def enforceLimit = synchronized {
    val overhead = size - limit
    if(overhead > 0) {
      val sortedFiles = cached.toList.
        filter{case(_, (_,_,usage)) => usage == 0 }.
        sorted(Ordering.by{l: (String, (File, Long, Int)) => l._2._2}).map{case(hash, (file, _,_)) => hash -> file}
        
      def remove(files: List[(String, File)], overhead: Long): Unit = 
        if(!(files.isEmpty || overhead <= 0)) {
          val (hash, file) = files.head
          val size = file.length
          file.delete
          cached -= hash
          remove(files.tail, overhead - size)
        }
      
    }
  }
  
}
