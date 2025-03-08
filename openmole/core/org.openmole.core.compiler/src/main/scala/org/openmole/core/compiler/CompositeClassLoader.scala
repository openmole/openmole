/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.compiler

import java.io.InputStream
import java.net.URL
import java.util
import scala.jdk.CollectionConverters._
import scala.util._

object CompositeClassLoader {

  def loop[T](classLoaders: Seq[ClassLoader])(f: ClassLoader => T, cl: List[ClassLoader] = classLoaders.toList): Option[T] = {
    cl match {
      case Nil => None
      case h :: t =>
        Try(f(h)) match {
          case Success(null) => loop(classLoaders)(f, t)
          case Failure(_)    => loop(classLoaders)(f, t)
          case Success(r)    => Some(r)
        }
    }
  }

}

class CompositeClassLoader(val classLoaders: ClassLoader*) extends ClassLoader {

  override def loadClass(s: String, b: Boolean): Class[?] =
    CompositeClassLoader.loop(classLoaders)(_.loadClass(s)).getOrElse(throw new ClassNotFoundException)

  override def getResource(s: String): URL =
    CompositeClassLoader.loop(classLoaders)(_.getResource(s)).getOrElse(null)

  override def getResources(s: String): util.Enumeration[URL] = {
    val ret = new java.util.Vector[URL]
    for {
      cl ← classLoaders
      r ← cl.getResources(s).asScala
    } ret.add(r)
    ret.elements()
  }

  override def getResourceAsStream(s: String): InputStream =
    CompositeClassLoader.loop(classLoaders)(_.getResourceAsStream(s)).getOrElse(null)

}
