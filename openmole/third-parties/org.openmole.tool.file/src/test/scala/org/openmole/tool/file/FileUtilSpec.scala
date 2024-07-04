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

import java.nio.file.{ Files, Paths }

import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.scalatest._

class FileUtilSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  "A string" should "be appended to the stream" in:
    val t = "TestString"
    val sis = new StringOutputStream
    try
      sis.append(t)
      sis.toString should equal(t)
    finally sis.close()

  "A string" should "be appended to the file" in {
    val file = Files.createTempFile("test", ".tmp").toFile
    try {
      val t1 = "TestString"
      val t2 = "Test2String"
      file.withLock(_.append(t1))
      file.withLock(_.append(t2))
      file.content should equal(t1 + t2)
    }
    finally file.delete
  }

  "A broken symbolic link" should "be reported as such" in {
    val brokenPath = Paths.get("/tmp/gibberish")
    val link = Paths.get("/tmp/linktarget").toFile
    link createLinkTo brokenPath
    try {
      link.isSymbolicLink should equal(true)
      link.isBrokenSymbolicLink should equal(true)
    }
    finally {
      link.delete()
    }
  }

  "A valid symbolic link" should "be reported as such" in {
    val file = Files.createTempFile("test", ".tmp").toFile
    val link = Paths.get("/tmp/linktarget").toFile
    link createLinkTo file
    try {
      link.isSymbolicLink should equal(true)
      link.isBrokenSymbolicLink should equal(false)
    }
    finally {
      link.delete()
      file.delete()
    }
  }

}
