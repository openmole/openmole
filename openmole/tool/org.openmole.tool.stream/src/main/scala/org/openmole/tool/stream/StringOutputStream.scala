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

import java.io.{ OutputStream, PrintStream }

import org.apache.commons.collections4.queue._
import collection.JavaConverters._

trait Builder {
  def append(c: Char): Unit
  def clear(): String
  def toString: String
}

class SynchronizedBuilder() extends Builder {
  lazy val builder = new StringBuilder
  def append(c: Char) = builder.synchronized { builder.append(c) }
  def clear(): String = builder.synchronized {
    val content = builder.mkString
    builder.clear()
    content
  }
  override def toString = builder.synchronized { builder.mkString }
}

class SynchronizedRingBuilder(size: Int) extends Builder {
  lazy val buffer = new CircularFifoQueue[Char](size)

  def append(c: Char) = buffer.synchronized { buffer.add(c) }

  def clear(): String = buffer.synchronized {
    val content = buffer.iterator().asScala.toArray.mkString
    buffer.clear()
    content
  }

  override def toString = buffer.synchronized { buffer.iterator().asScala.toArray.mkString }

}

class StringOutputStream(maxCharacters: Option[Int] = None) extends OutputStream {
  lazy val builder =
    maxCharacters match {
      case None    => new SynchronizedBuilder
      case Some(n) => new SynchronizedRingBuilder(n)
    }

  override def write(b: Int) = builder.append(b.toChar)

  def clear(): String = {
    flush()
    builder.clear()
  }

  override def toString: String = builder.toString
}

object StringPrintStream {

  def apply(maxCharacters: Option[Int] = None) =
    new StringPrintStream(new StringOutputStream(maxCharacters))

}

class StringPrintStream(os: StringOutputStream) extends PrintStream(os) {
  def clear(): String = os.clear()

  override def toString = {
    flush()
    os.toString
  }

}