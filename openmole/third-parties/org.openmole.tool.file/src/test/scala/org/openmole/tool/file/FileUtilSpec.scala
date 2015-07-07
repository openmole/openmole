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

package org.openmole.tool.file

import java.io.File
import java.io.FileWriter
import scala.io.Source
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.scalatest.FlatSpec
import org.scalatest._

class FileUtilSpec extends FlatSpec with Matchers {

  "A string" should "be append to the stream" in {
    val t = "TestString"
    val sis = new StringOutputStream
    try {
      sis.append(t)
      sis.toString should equal(t)
    }
    finally sis.close
  }

  "A string" should "be append to the file" in {
    val file = File.createTempFile("test", ".tmp")
    try {
      val t1 = "TestString"
      val t2 = "Test2String"
      file.withLock(_.append(t1))
      file.withLock(_.append(t2))
      file.content should equal(t1 + t2)
    }
    finally file.delete
  }

}
