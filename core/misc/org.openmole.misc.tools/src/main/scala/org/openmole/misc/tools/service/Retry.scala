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

package org.openmole.misc.tools.service

import java.util.concurrent.TimeoutException
import java.net.SocketTimeoutException

object Retry {

  def retryOnTimeout[T](f: ⇒ T, nbTry: Int): T = {
    def retryOrElse[T](f: ⇒ T): T = if (nbTry > 1) retryOnTimeout(f, nbTry - 1) else f
    try f
    catch {
      case t: TimeoutException ⇒ retryOrElse(throw t)
      case t: SocketTimeoutException ⇒ retryOrElse(throw t)
    }
  }

  def retry[T](f: ⇒ T, nbTry: Int): T =
    try f
    catch {
      case t: Throwable ⇒
        if (nbTry > 1) retry(f, nbTry - 1)
        else throw t
    }

  //  def waitAndRetryFor[T](callable: ⇒ T, nbTry: Int, exceptions: Set[Class[_]], wait: Long): T = {
  //    var _nbTry = nbTry - 1
  //    while (_nbTry <= 0) {
  //      try {
  //        return callable
  //      } catch {
  //        case e: Throwable ⇒
  //          if (!exceptions.contains(e.getClass)) throw e
  //          Thread.sleep(wait)
  //      }
  //      _nbTry -= 1
  //    }
  //    callable
  //  }

}
