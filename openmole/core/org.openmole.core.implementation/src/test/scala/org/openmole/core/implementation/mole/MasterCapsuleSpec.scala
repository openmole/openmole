/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation.mole

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IContext
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.sampling.ExplicitSampling
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.ExplorationTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.transition.EndExplorationTransition
import org.openmole.core.implementation.transition.ExplorationTransition
import org.openmole.core.implementation.transition.Slot
import org.openmole.core.implementation.transition.Transition
import org.openmole.core.model.data.DataModeMask._

@RunWith(classOf[JUnitRunner])
class MasterCapsuleSpec extends FlatSpec with ShouldMatchers {
  
  "A master capsule" should "execute tasks" in {
    val p = new Prototype("p", classOf[String])
    
    val t1 = new Task("Test write") {
      override def process(context: IContext) = context + (p -> "Test")
    }
    
    t1.addOutput(p)
    
    val t2 = new Task("Test read") {
      override def process(context: IContext) = {
        context.value(p).get should equal ("Test")
        context
      }
    }
    
    t2.addInput(p)
    
    val t1c = new MasterCapsule(t1)
    val t2c = new MasterCapsule(t2)
    
    new Transition(t1c, t2c)
    
    new MoleExecution(new Mole(t1c)).start.waitUntilEnded
  }
  
  "A master capsule" should "keep value of a variable from on execution to another" in {
    val data = List("A","A","B","C")
    val i = new Prototype("i", classOf[String])
    val n = new Prototype("n", classOf[Int])
    
    val sampling = new ExplicitSampling(i, data)
    
    val exc = new Capsule(new ExplorationTask("Exploration", sampling))
     
    val emptyT = new EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)
    
    val emptyC = new Capsule(emptyT)
     
    val select = new Task("select") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        context + new Variable(n, nVal + 1) + new Variable(i, (nVal + 1).toString)
      }
    }
    
    select.addParameter(n, 0)
    
    select.addInput(n)
    select.addInput(i)
    
    select.addOutput(n)
    select.addOutput(i)
    
    val selectCaps = new MasterCapsule(select, n)

    new ExplorationTransition(exc, emptyC)
    new Transition(emptyC, selectCaps)
    new Transition(selectCaps, new Slot(emptyC), "n <= 100")              
    
    new MoleExecution(new Mole(exc)).start.waitUntilEnded 
  }
  
  "A end of exploration transition" should "end the master slave process" in {
    var endCapsExecuted = 0
    
    val data = List("A","A","B","C")
    val i = new Prototype("i", classOf[String])
    val isaved = new Prototype("isaved", classOf[Array[String]])
    val n = new Prototype("n", classOf[Int])
    
    val sampling = new ExplicitSampling(i, data)
    
    val exc = new Capsule(new ExplorationTask("Exploration", sampling))
     
    val emptyT = new EmptyTask("Slave")
    emptyT.addInput(i)
    emptyT.addOutput(i)
    
    val emptyC = new Capsule(emptyT)
    
    val testT = new Task("Test") {
      override def process(context: IContext) = {
        context.contains(isaved) should equal (true)
        context.value(isaved).get.size should equal (10)
        endCapsExecuted += 1
        context
      }
    }
    
    testT.addInput(isaved)
    
    val testC = new Capsule(testT)
     
    val select = new Task("select") {
      override def process(context: IContext) = {
        val nVal = context.value(n).get
        val isavedVar = (nVal.toString :: context.variable(isaved).get.value.toList)

        context + 
          (if(isavedVar.size > 10) new Variable(isaved, isavedVar.tail.toArray)
          else new Variable(isaved, isavedVar.toArray)) + new Variable(n, nVal + 1)
      }
    }
    
    select.addParameter(n, 0)
    select.addParameter(isaved, Array.empty[String])
    
    select.addInput(n)
    select.addInput(isaved)
    select.addInput(i)
    
    select.addOutput(isaved)
    select.addOutput(n)
    select.addOutput(i)

    val selectCaps = new MasterCapsule(select, n, isaved)
    
    new ExplorationTransition(exc, emptyC)
    new Transition(emptyC, selectCaps)
    new Transition(selectCaps, new Slot(emptyC))  
    new EndExplorationTransition("n >= 100", selectCaps, testC)

    new MoleExecution(new Mole(exc)).start.waitUntilEnded 
    endCapsExecuted should equal (1)
  }
  
  
}
