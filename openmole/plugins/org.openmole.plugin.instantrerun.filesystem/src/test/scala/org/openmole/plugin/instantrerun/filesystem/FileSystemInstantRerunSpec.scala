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

package org.openmole.plugin.instantrerun.filesystem

import org.openmole.misc.workspace.Workspace
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IContext

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.io.File

import org.openmole.misc.tools.io.FileUtil._
import scala.collection.mutable.ListBuffer

@RunWith(classOf[JUnitRunner])
class FileSystemInstantRerunSpec extends FlatSpec with ShouldMatchers {

  "The instant rerun" should "avoid the second execution of the task" in {
   val p = new Prototype("p", classOf[java.lang.Long])
   val res = new ListBuffer[Long]
    
    val t1 = new Task {
      val name = "Test instant rerun"
      override def process(context: IContext) = context + (p -> new java.lang.Long(System.currentTimeMillis))
    }
    
    val t2 = new Task {
      val name = "Add result"
      override def process(context: IContext) = {
        res += context.value(p).get
        context
      }
    }
    
    t1.addOutput(p)
    t2.addInput(p)
    
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    
    new Transition(t1c, t2c)
    
    val dir = File.createTempFile("testInstantRerun", "")
    dir.delete
    dir.mkdir
    
    new MoleExecution(new Mole(t1c), rerun = new FileSystemInstantRerun(dir, t1c)).start.waitUntilEnded
    new MoleExecution(new Mole(t1c), rerun = new FileSystemInstantRerun(dir, t1c)).start.waitUntilEnded
    dir.recursiveDelete
    
    res.distinct.size should equal (1)
  }
  
}
