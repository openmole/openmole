/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.plugin.task.filemanagement

import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.execution.Progress
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.io.File
import org.junit.runner.RunWith
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class DeleteFileTaskSpec extends FlatSpec with ShouldMatchers {

  "A delete file task" should "delete all the files contained in a File prototype" in {
    val f1 = File.createTempFile("file", ".test")

    val p1 = new Prototype("file1", classOf[File])
    
    val t1 = new DeleteFileTask("Test DeleteFileTask")
    t1.deleteInputFile(p1)
    
    val context = new Context()
    context +=  p1 -> f1
    
    t1.process(context, new Progress())
    
    f1.exists should equal (false)
  }
    
  
  "A delete file task" should "delete all the files contained in an array of File prototypes" in {
    val a1 = Array(
      File.createTempFile("file", ".test"),
      File.createTempFile("file", ".test"),
      File.createTempFile("file", ".test")
    )

    val p1 = new Prototype("array1",classOf[Array[File]])
   
    val context = new Context
    context +=  p1 -> a1

    val t1 = new DeleteFileTask("Test DeleteFileTask")
    t1.deleteInputFileList(p1)

    t1.process(context, new Progress)
    
    a1.find(_.exists).isDefined should equal (false)
  }
}

