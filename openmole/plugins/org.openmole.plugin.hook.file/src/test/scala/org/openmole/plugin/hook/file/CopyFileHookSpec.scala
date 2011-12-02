/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.hook.file

import java.io.File
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.misc.hashservice.HashService._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.io.FileWriter
import org.junit.runner.RunWith
import scala.io.Source


@RunWith(classOf[JUnitRunner])
class CopyFileHookSpec extends FlatSpec with ShouldMatchers {
  
  "A copy file hook" should "copy a file after the execution of a capsule" in {
    val f = File.createTempFile("test", ".tmp")
    
    val fw = new FileWriter(f)
    try fw.write("File contents!")
    finally fw.close
    
    val p = new Prototype("p", classOf[File])
    
    val t1 = new Task("Test") {
      override def process(context: IContext) = context + (p -> f)
    }

    t1.addOutput(p)
    
    val t1c = new Capsule(t1)
    val ex = new MoleExecution(new Mole(t1c))
    
    val fDest = File.createTempFile("test", ".tmp")
    
    val hook = new CopyFileHook(ex, t1c, p, fDest.getAbsolutePath)
    
    ex.start.waitUntilEnded
    
    f.hash should equal (fDest.hash)
    f.delete
    fDest.delete
  }
  
}
