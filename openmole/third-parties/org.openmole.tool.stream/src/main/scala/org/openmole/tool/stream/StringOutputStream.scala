/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.tool.stream

import java.io.{ PrintStream, OutputStream }

class SynchronizedBuilder {
  lazy val builder = new StringBuilder
  def append(c: Char) = builder.synchronized { builder.append(c) }
  def read: String = builder.synchronized {
    val content = builder.toString()
    builder.clear()
    content
  }
  override def toString = builder.toString()
}

class StringOutputStream extends OutputStream {
  lazy val builder = new SynchronizedBuilder
  override def write(b: Int) = builder.append(b.toChar)

  def read: String = {
    flush()
    builder.read
  }

  override def toString: String = builder.toString
}

class StringPrintStream(val os: StringOutputStream = new StringOutputStream) extends PrintStream(os) {
  def read: String = {
    flush()
    os.read
  }

  override def toString = os.toString
}