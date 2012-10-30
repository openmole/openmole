/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.misc.tools.service

import scala.collection.mutable.HashMap

class ReadWriteLock {

  private val readingThreads = new HashMap[Thread, Int]
  private var writeAccesses = 0
  private var writeRequests = 0
  private var writingThread: Option[Thread] = None

  def lockRead = synchronized {
    val callingThread = Thread.currentThread
    while (!canGrantReadAccess(callingThread)) wait
    readingThreads.put(callingThread, (getReadAccessCount(callingThread) + 1))
  }

  private def canGrantReadAccess(callingThread: Thread): Boolean = {
    if (isWriter(callingThread)) return true
    if (hasWriter) return false
    if (isReader(callingThread)) return true
    if (hasWriteRequests) return false
    return true
  }

  def unlockRead = synchronized {
    val callingThread = Thread.currentThread
    if (!isReader(callingThread)) {
      throw new IllegalMonitorStateException("Calling Thread does not hold a read lock on this ReadWriteLock")
    }
    val accessCount = getReadAccessCount(callingThread)
    if (accessCount == 1) readingThreads.remove(callingThread)
    else readingThreads.put(callingThread, (accessCount - 1))
    notifyAll
  }

  def lockWrite = synchronized {
    writeRequests += 1
    val callingThread = Thread.currentThread
    while (!canGrantWriteAccess(callingThread)) wait

    writeRequests -= 1
    writeAccesses += 1
    writingThread = Some(callingThread)
  }

  def unlockWrite = synchronized {
    if (!isWriter(Thread.currentThread)) {
      throw new IllegalMonitorStateException("Calling Thread does not hold the write lock on this ReadWriteLock")
    }
    writeAccesses -= 1
    if (writeAccesses == 0) writingThread = None

    notifyAll
  }

  private def canGrantWriteAccess(callingThread: Thread): Boolean = {
    if (isOnlyReader(callingThread)) return true
    if (hasReaders) return false
    if (!writingThread.isDefined) return true
    if (!isWriter(callingThread)) return false
    return true
  }

  private def getReadAccessCount(callingThread: Thread): Int = readingThreads.getOrElse(callingThread, 0)

  private def hasReaders = readingThreads.size > 0

  private def isReader(callingThread: Thread) = readingThreads.contains(callingThread)

  private def isOnlyReader(callingThread: Thread) = readingThreads.size == 1 && readingThreads.contains(callingThread)

  private def hasWriter = writingThread.isDefined

  private def isWriter(callingThread: Thread) = {
    writingThread match {
      case None ⇒ false
      case Some(t) ⇒ t == callingThread
    }
  }

  private def hasWriteRequests = this.writeRequests > 0

}

