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

import java.io.File
import org.openmole.misc.tools.io.FileUtil._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.io.FileWriter
import org.junit.runner.RunWith
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class FileUtilSpec extends FlatSpec with ShouldMatchers {

  "A string" should "be append to the stream" in {
    val t = "TestString"
    val sbuild = new StringBuilder
    val sis = new StringBuilderOutputStream(sbuild)
    try sis.append(t)
    finally sis.close
    sbuild.toString should equal(t)
  }

  "A string" should "be append to the file" in {
    val file = File.createTempFile("test", ".tmp")
    try {
      val t1 = "TestString"
      val t2 = "Test2String"
      file.withLock(_.append(t1))
      file.withLock(_.append(t2))
      file.content should equal(t1 + t2)
    } finally file.delete
  }

}
