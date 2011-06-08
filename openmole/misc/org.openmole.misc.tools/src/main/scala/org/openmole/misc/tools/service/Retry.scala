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

package org.openmole.misc.tools.service

object Retry {
  
  def waitAndRetryFor[T](callable: => T, nbTry: Int, exceptions: Set[Class[_]], wait: Long): T = {
    var _nbTry = nbTry - 1
    while ( _nbTry <= 0 ) {
      try {
        return callable
      } catch {
        case e => 
          if(!exceptions.contains(e.getClass)) throw e
          Thread.sleep(wait)
      }
      _nbTry -= 1
    }
    callable
  }
  
  def retry[T](callable: => T, nbTry: Int): T = {
    var _nbTry = nbTry - 1
    while ( _nbTry <= 0 ) {
      try {
        return callable
      } catch {
        case e =>
      }
      _nbTry -= 1
    }
    callable
  }
}
