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

package org.openmole.misc.tools.service

class TimeCache[T] {

  var cache: Option[T] = None
  var cacheTime = System.currentTimeMillis

  def apply(f: ⇒ T, ms: Long) = synchronized {
    if (cacheTime + ms < System.currentTimeMillis) cache = None
    cache match {
      case None ⇒
        cache = Some(f)
        cacheTime = System.currentTimeMillis
        cache.get
      case Some(c) ⇒ c
    }
  }

}
