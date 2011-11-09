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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.model.data.IContext
import org.openmole.core.model.sampling.ISampling
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer
import org.openmole.core.model.data.DataModeMask._

@RunWith(classOf[JUnitRunner])
class MasterTransitionSpec extends FlatSpec with ShouldMatchers {
  
  "Master transition" should "resubmit 10 jobs" in {
    var endCapsExecuted = 0
    
    val data = List("A","A","B","C")
    val i = new Prototype("i", classOf[String])
    val n = new Prototype("n", classOf[Int])
    
    val sampling = new ExplicitSampling(i, data)
    
    val exc = new Capsule(new ExplorationTask("Exploration", sampling))
     
    val emptyT = new EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)
    
    val emptyC = new Capsule(emptyT)
    
    val testT = new Task("Test") {
      override def process(context: IContext) = {
        context.contains(toArray(i)) should equal (true)
        context.value(toArray(i)).get.size should equal (10)
        endCapsExecuted += 1
        context
      }
    }
    
    testT.addInput(toArray(i))
    
    val testC = new Capsule(testT)
     
    val select = new Task("select") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        
        (if(context.value(toArray(i)).get.size > data.size) context + (n, nVal + 1) else context) + (toArray(i), context.value(toArray(i)).get.slice(0, 10))
      }
    }
    
    select.addParameter(n, 0)
    select.addInput(n)
    select.addOutput(n)
    select.addInput(toArray(i))
    select.addOutput(toArray(i))
    
    val master = new Task("master") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        context + (i, nVal.toString)
      }
    }
    
    master.addInput(n)
    master.addInput(toArray(i))
    master.addOutput(i)

    val masterCaps = new Capsule(master)
    
    
    new ExplorationTransition(exc, emptyC)
    new MasterTransition(new Master(select, masterCaps), "n >= 10", emptyC, testC)
    new Transition(masterCaps, new Slot(emptyC))              
    
    
    new MoleExecution(new Mole(exc)).start.waitUntilEnded 
    endCapsExecuted should equal (1)
  }
  
  "Master slave transition" should "resubmit 10 jobs" in {
    var slaveExecuted = 0
    var endCapsExecuted = 0
    
    val data = List("A","A","B","C")
    val i = new Prototype("i", classOf[String])
    val n = new Prototype("n", classOf[Int])
    
    val sampling = new ExplicitSampling(i, data)
    
    val exc = new Capsule(new ExplorationTask("Exploration", sampling))
     
    val emptyT = new EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)
    
    val emptyC = new Capsule(emptyT)
    
    val testT = new Task("Test") {
      override def process(context: IContext) = {
        context.contains(toArray(i)) should equal (true)
        context.value(toArray(i)).get.size should equal (10)
        endCapsExecuted += 1
        context
      }
    }
    
    testT.addInput(toArray(i))
    
    val testC = new Capsule(testT)
     
    val select = new Task("select") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        (if(context.value(toArray(i)).get.size > data.size) context + (n, nVal + 1) else context) + (toArray(i), context.value(toArray(i)).get.slice(0, 10))
      }
    }
    
    select.addParameter(n, 0)
    select.addInput(n)
    select.addOutput(n)
    select.addInput(toArray(i))
    select.addOutput(toArray(i))
    
    val master = new Task("master") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        context + (toArray(i), Array(nVal.toString))
      }
    }
    
    master.addInput(n)
    master.addInput(toArray(i))
    master.addOutput(toArray(i), Array(explore))

    val masterCaps = new Capsule(master)
    
    
    new ExplorationTransition(exc, emptyC)
    new MasterTransition(new Master(select, masterCaps), "n >= 10", emptyC, testC)
    new SlaveTransition(masterCaps, new Slot(emptyC))              
    
    
    new MoleExecution(new Mole(exc)).start.waitUntilEnded 
    endCapsExecuted should equal (1)
   // slaveExecuted should equal (data.size + 10)
  }

}
