/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.misc.workspace

import com.thoughtworks.xstream.XStream
import java.io.File
import org.openmole.misc.tools.io.FileUtil._

object PersistentList {
  val pattern = "[0-9]+"
}

class PersistentList[T](serializer: XStream, dir: File) extends Iterable[(Int, T)] {
  
  def file(i: Int) = new File(dir, i.toString)
  
  def -= (i: Int) = file(i).delete
  
  def apply(i: Int): T = serializer.fromXML(file(i).content).asInstanceOf[T]
  
  def update(i: Int, obj: T) = file(i).content = serializer.toXML(obj)
  
  override def iterator = 
    dir.listFiles{f: File => f.getName.matches(PersistentList.pattern)}.map{_.getName.toInt}.sorted.map{i => i -> apply(i)}.iterator
}
