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

package org.openmole.core.implementation.transition

import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.mole.FixedEnvironmentSelection
import org.openmole.core.implementation.mole.Mole
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.implementation.task.EmptyTask
import org.openmole.core.implementation.task.Task
import org.openmole.core.implementation.mole.Capsule
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IContext

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TransitionSpec extends FlatSpec with ShouldMatchers {

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = new Prototype("p", classOf[String])
    
    val t1 = new Task {
      val name = "Test write"
      override def process(context: IContext) = context + (p -> "Test")
    }
    
    t1.addOutput(p)
    
    val t2 = new Task {
      val name = "Test read"
      override def process(context: IContext) = {
        context.value(p).get should equal ("Test")
        context
      }
    }
    
    t2.addInput(p)
    
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    
    new Transition(t1c, t2c)
    
    new MoleExecution(new Mole(t1c)).start.waitUntilEnded
  }
  
  
  "A conjonctive pattern" should "enable variable values to be transmitted from a task to another" in {
    val p1 = new Prototype("p1", classOf[String])
    val p2 = new Prototype("p2", classOf[String])
    
    val init = new EmptyTask("Init")
    
    val t1 = new Task {
      val name = "Test write 1"
      override def process(context: IContext) = context + (p1 -> "Test1")
    }
    
    t1.addOutput(p1)
    
    val t2 = new Task {
      val name = "Test write 2"
      override def process(context: IContext) = context + (p2 -> "Test2")
    }
    
    t2.addOutput(p2)
    
    val t3 = new Task {
      val name = "Test read"
      override def process(context: IContext) = {
        context.value(p1).get should equal ("Test1")
        context.value(p2).get should equal ("Test2") 
        context
      }
    }
    
    t3.addInput(p1)
    t3.addInput(p2)
    
    val initc = new Capsule(init)
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = new Capsule(t3)
    
    new Transition(initc, t1c)
    new Transition(initc, t2c)
    new Transition(t1c, t3c)
    new Transition(t2c, t3c)
    
    new MoleExecution(new Mole(initc)).start.waitUntilEnded
  
  }
  
  "A conjonctive pattern" should "aggregate variable of the same name in an array of closest common supertype" in {
        

    val p1 = new Prototype("p", classOf[java.lang.Long])
    val p2 = new Prototype("p", classOf[java.lang.Integer])
    val pArray = new Prototype("p", classOf[Array[java.lang.Number]])
    
    val init = new EmptyTask("Init")
    
    val t1 = new Task {
      val name = "Test write 1"
      override def process(context: IContext) = context + (p1 -> new java.lang.Long(1L))
    }
    
    t1.addOutput(p1)
    
    val t2 = new Task {
      val name = "Test write 2"
      override def process(context: IContext) = context + (p2 -> new java.lang.Integer(2))
    }
    
    t2.addOutput(p2)
    
    val t3 = new Task {
      val name = "Test read"
      override def process(context: IContext) = {
        //println(context.value(pArtoStringray).map(_.intL))
        context.value(pArray).get.map(_.intValue).contains(1) should equal (true)
        context.value(pArray).get.map(_.intValue).contains(2) should equal (true)
  
        context.value(pArray).get.getClass should equal (classOf[Array[java.lang.Number]])
        context
      }
    }
    
    
    t3.addInput(pArray)
    
    val initc = new Capsule(init)
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = new Capsule(t3)
    
    new Transition(initc, t1c)
    new Transition(initc, t2c)
    new Transition(t1c, t3c)
    new Transition(t2c, t3c)
    
    new MoleExecution(new Mole(initc)).start.waitUntilEnded
    
  }
    
  "A conjonctive pattern" should "be robust to concurent execution" in {
    var executed = false
    val p1 = new Prototype("p1", classOf[String])
    val p2 = new Prototype("p2", classOf[String])
    
    val init = new EmptyTask("Init")
    
    val t1 = new Task {
      val name = "Test write 1"
      override def process(context: IContext) = context + (p1 -> "Test1")
    }
    
    t1.addOutput(p1)
    
    val t2 = new Task {
      val name = "Test write 2"
      override def process(context: IContext) = context + (p2 -> "Test2")
    }
    
    t2.addOutput(p2)
    
    val t3 = new Task {
      val name = "Test read"
      override def process(context: IContext) = {
        context.value(p1).get should equal ("Test1")
        context.value(toArray(p2)).get.head should equal ("Test2") 
        context.value(toArray(p2)).get.size should equal (100)
        executed = true
        context
      }
    }
    
    t3.addInput(p1)
    t3.addInput(toArray(p2))
    
    val initc = new Capsule(init)
    val t1c = new Capsule(t1)

    val t3c = new Capsule(t3)
    
    val t2CList = (0 until 100).map { i => 
      val t2c = new Capsule(t2)
      new Transition(initc, t2c)
      new Transition(t2c, t3c)   
      t2c
    }
    
    new Transition(initc, t1c)
    new Transition(t1c, t3c)

    val env = new LocalExecutionEnvironment(20)
    
    new MoleExecution(
      new Mole(initc), 
      FixedEnvironmentSelection(t2CList.map{_ -> env}: _*)
    ).start.waitUntilEnded
    executed should equal (true)
  }
  
  "A conjonctive pattern" should "aggregate array variable of the same name in an array of array of the closest common supertype" in {
    
    val p1 = new Prototype("p", classOf[Array[java.lang.Long]])
    val p2 = new Prototype("p", classOf[Array[java.lang.Integer]])
    val pArray = new Prototype("p", classOf[Array[Array[java.lang.Number]]])
    
    val init = new EmptyTask("Init")
    
    val t1 = new Task {
      val name = "Test write 1"
      override def process(context: IContext) = context + (p1 -> Array(new java.lang.Long(1L)))
    }
    
    t1.addOutput(p1)
    
    val t2 = new Task {
      val name = "Test write 2"
      override def process(context: IContext) = context + (p2 -> Array(new java.lang.Integer(2)))
    }
    
    t2.addOutput(p2)
    
    val t3 = new Task {
      val name = "Test read"
      override def process(context: IContext) = {
        val res = IndexedSeq(context.value(pArray).get(0).deep, context.value(pArray).get(1).deep)
        
        res.contains(Array(new java.lang.Integer(1)).deep) should equal (true)
        res.contains(Array(new java.lang.Long(2L)).deep) should equal (true)
  
        context.value(pArray).get.getClass should equal (classOf[Array[Array[java.lang.Number]]])
        context
      }
    }
    
    
    t3.addInput(pArray)
    
    val initc = new Capsule(init)
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = new Capsule(t3)
    
    new Transition(initc, t1c)
    new Transition(initc, t2c)
    new Transition(t1c, t3c)
    new Transition(t2c, t3c)
    
    new MoleExecution(new Mole(initc)).start.waitUntilEnded
  }
  
  
  
  
}
