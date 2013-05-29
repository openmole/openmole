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

package org.openmole.core.serializer.converter

import collection.mutable
import com.thoughtworks.xstream.XStream
import collection.mutable.ListBuffer

object Factory {

  trait XStreamPool {
    def clean
    def xStreams: Iterable[XStream]
  }

}

import Factory._

trait Factory {

  private val pool = new mutable.Stack[T]
  private val _instantiated = ListBuffer.empty[T]

  type T <: XStreamPool

  def make: T

  def initialize(t: T): T = t

  def instantiated = synchronized(_instantiated.toList)

  def borrow: T = synchronized {
    if (!pool.isEmpty) pool.pop
    else {
      val t = initialize(make)
      _instantiated += t
      t
    }
  }

  def release(serial: T) = synchronized {
    try serial.clean
    finally pool.push(serial)
  }

  def exec[A](f: T ⇒ A): A = {
    val o = borrow
    try f(o)
    finally release(o)
  }
}
