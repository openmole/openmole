/*
 * Copyright (C) 23/09/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workspace

import org.openmole.tool.file._
import scala.util.Try
import java.io.File

object Authentication {
  val pattern = "[-0-9A-z]+\\.key"
}

trait Authentication <: Persistent {

  def size[T](implicit m: Manifest[T]): Int = size(category[T])

  def category[T](implicit m: Manifest[T]): String = m.runtimeClass.getCanonicalName

  def save[T](fileName: String, obj: T)(implicit m: Manifest[T]): Unit =
    save(obj, fileName, Some(category[T]))

  def clean[T](implicit m: Manifest[T]): Unit = super.clean(Some(m.runtimeClass.getCanonicalName))

  def allByCategory = synchronized {
    baseDir.listFiles { f: File ⇒ f.isDirectory }.map {
      d ⇒
        d.getName -> d.listFiles { f: File ⇒ f.getName.matches(Authentication.pattern) }.flatMap {
          f ⇒
            Try(Persistent.xstream.fromXML(f.content)).toOption match {
              case Some(t: Any) ⇒ Some(t, f.getName)
              case _            ⇒ None
            }
        }.toSeq
    }.toMap
  }
}
