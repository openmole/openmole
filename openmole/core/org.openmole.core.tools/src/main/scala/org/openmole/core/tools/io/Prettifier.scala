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

package org.openmole.core.tools.io

import java.io.{ PrintWriter, StringWriter }

import org.openmole.tool.logger.JavaLogger

import scala.jdk.CollectionConverters._

object Prettifier extends JavaLogger {

  import Log._

  implicit class objectPrettifier(o: Any) {
    def prettify(snipArray: Int = Int.MaxValue): String = Prettifier.prettify(o, snipArray)
  }

  def snip[T <: Any](o: Iterable[T], size: Int = Int.MaxValue) =
    "[" +
      (if (o.size <= size) o.map { e ⇒ prettify(e, size) }.mkString(", ")
      else o.take(size - 1).map { e ⇒ prettify(e, size) }.mkString(", ") + "..., " + o.last) +
      "]"

  def prettify(o: Any, snipArray: Int = Int.MaxValue): String =
    try o match {
      case null                       ⇒ "null"
      case o: Array[?]                ⇒ snip(o, snipArray)
      case o: Seq[_]                  ⇒ snip(o, snipArray)
      case o: java.util.Collection[_] ⇒ snip(o.asScala, snipArray)
      case o                          ⇒ o.toString
    } catch {
      case t: Throwable ⇒
        logger.log(WARNING, "Error during pretification", t)
        o.toString
    }

  implicit class ExceptionPretiffier(t: Throwable) {

    def addMargin(s: String) = s.split("\n").map(" | " + _).mkString("\n")

    def stackString: String = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      t.printStackTrace(pw)
      sw.toString
    }

    def stackStringWithMargin = addMargin(stackString)
  }

}
