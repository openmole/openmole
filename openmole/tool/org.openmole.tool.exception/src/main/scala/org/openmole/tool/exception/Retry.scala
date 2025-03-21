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

package org.openmole.tool.exception

import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import squants.time.*

import scala.annotation.tailrec

object Retry:

  def retryOnTimeout[T](f: => T, nbTry: Int): T =
    def retryOrElse[T](f: => T): T = if (nbTry > 1) retryOnTimeout(f, nbTry - 1) else f
    try f
    catch
      case t: TimeoutException       => retryOrElse(throw t)
      case t: SocketTimeoutException => retryOrElse(throw t)

  @tailrec def retry[T](f: => T, nbTry: Int, coolDown: Option[Time] = None): T =
    try f
    catch
      case t: Throwable =>
        if nbTry > 1
        then
          coolDown.foreach(c => Thread.sleep(c.millis))
          retry(f, nbTry - 1)
        else throw t

  def retry[T](nbTry: Int)(f: => T): T = retry(f, nbTry)

