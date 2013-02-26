/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.misc.tools.io

import collection.JavaConversions._
import org.openmole.misc.tools.service.Logger

object Prettifier extends Logger {

  implicit def objectPrettifer(o: Any) = new {
    def prettify(snipArray: Int = Int.MaxValue) = Prettifier.prettify(o, snipArray)
  }

  def snip[T <: Any](o: Iterable[T], size: Int = Int.MaxValue) =
    "[" +
      (if (o.size <= size) o.map { e ⇒ prettify(e, size) }.mkString(", ")
      else o.take(size - 1).map { e ⇒ prettify(e, size) }.mkString(", ") + "..., " + o.last) +
      "]"

  def prettify(o: Any, snipArray: Int = Int.MaxValue): String =
    try o match {
      case null ⇒ "null"
      case o: Array[_] ⇒ snip(o, snipArray)
      case o: Iterable[_] ⇒ snip(o, snipArray)
      case o: java.lang.Iterable[_] ⇒ snip(o, snipArray)
      case o ⇒ o.toString
    } catch {
      case t: Throwable ⇒
        logger.log(WARNING, "Error during pretification", t)
        o.toString
    }

}
