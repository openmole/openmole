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

package org.openmole.core.implementation.hook

import org.openmole.core.implementation.mole.{Capsule, MasterCapsule}
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.model.data.IContext
import org.openmole.core.model.job.IMoleJob
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class CapsuleExecutionHookSpec extends FlatSpec with ShouldMatchers {
  
  "A capsule execution hook" should "intersept the execution of a capsule" in {
    var executed = false
    
    val p = new Prototype("p", classOf[String])
    
    val t1 = new Task {
      val name = "Test"
      override def process(context: IContext) = context + (p -> "test")
    }
    
    t1.addOutput(p)
    
    val t1c = new Capsule(t1)
    val ex = new MoleExecution(new Mole(t1c))
    
    new CapsuleExecutionHook(ex, t1c) {
      override def process(moleJob: IMoleJob) = {
        moleJob.context.contains(p) should equal (true)
        moleJob.context.value(p).get should equal ("test")
        executed = true
      }
    }
    
    ex.start.waitUntilEnded
    
    executed should equal (true)
  }
  
  "A capsule execution hook" should "intersept the execution of a master capsule" in {
    var executed = false
    
    val p = new Prototype("p", classOf[String])
    
    val t1 = new Task {
      val name = "Test"
      override def process(context: IContext) = context + (p -> "test")
      
    }
    
    t1.addOutput(p)
    
    val t1c = new MasterCapsule(t1)
    val ex = new MoleExecution(new Mole(t1c))
    
    new CapsuleExecutionHook(ex, t1c) {
      override def process(moleJob: IMoleJob) = {
        moleJob.context.contains(p) should equal (true)
        moleJob.context.value(p).get should equal ("test")
        executed = true
      }
    }
    
    ex.start.waitUntilEnded
    
    executed should equal (true)
  }
  
  
  
  
  "After release a capsule execution hook" should "not be executed" in {
    var executed = false
    
    val t1 = new EmptyTask("Test") 
    
    val t1c = new Capsule(t1)
    val ex = new MoleExecution(new Mole(t1c))
    
    val hook = new CapsuleExecutionHook(ex, t1c) {
      override def process(moleJob: IMoleJob) = {
        executed = true
      }
    }
    
    hook.release
    ex.start.waitUntilEnded
    
    executed should equal (false)
  }
  
}
