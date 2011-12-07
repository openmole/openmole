/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.tools.service

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

object ThreadUtil {
  val daemonThreadFactory = new ThreadFactory {

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
          override def uncaughtException(t: Thread, e: Throwable) = {
            println("Uncaught"); e.printStackTrace
          }
        })
      t
    }

  }
  
  def background[F](f: => F) = Executors.newSingleThreadExecutor(daemonThreadFactory).submit(new Runnable { def run { f } } )
}
