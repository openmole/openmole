/*
 * Copyright (C) 2011 Romain Reuillon
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
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.data.Prototype._
import org.openmole.core.model.data.IContext

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TransitionSpec extends FlatSpec with ShouldMatchers {

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = new Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: IContext) = context + (p -> "Test")
    }

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: IContext) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)

    new Transition(t1c, t2c)

    new MoleExecution(new Mole(t1c)).start.waitUntilEnded
  }

  "A conjonctive pattern" should "enable variable values to be transmitted from a task to another" in {
    val p1 = new Prototype[String]("p1")
    val p2 = new Prototype[String]("p2")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: IContext) = context + (p1 -> "Test1")
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: IContext) = context + (p2 -> "Test2")
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p1, p2)
      override def process(context: IContext) = {
        context.value(p1).get should equal("Test1")
        context.value(p2).get should equal("Test2")
        context
      }
    }

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

    val p1 = new Prototype[java.lang.Long]("p")
    val p2 = new Prototype[java.lang.Integer]("p")
    val pArray = new Prototype[Array[java.lang.Number]]("p")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: IContext) = context + (p1 -> new java.lang.Long(1L))
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: IContext) = context + (p2 -> new java.lang.Integer(2))
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(pArray)
      override def process(context: IContext) = {
        //println(context.value(pArtoStringray).map(_.intL))
        context.value(pArray).get.map(_.intValue).contains(1) should equal(true)
        context.value(pArray).get.map(_.intValue).contains(2) should equal(true)

        context.value(pArray).get.getClass should equal(classOf[Array[java.lang.Number]])
        context
      }
    }

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
    @volatile var executed = 0
    val p1 = new Prototype[String]("p1")
    val p2 = new Prototype[String]("p2")

    val init = EmptyTask("Init conjonctive")

    val t1 = new TestTask {
      val name = "Test write 1 conjonctive"
      override def outputs = DataSet(p1)
      override def process(context: IContext) = context + (p1 -> "Test1")
    }

    val t2 = new TestTask {
      val name = "Test write 2 conjonctive"
      override def outputs = DataSet(p2)
      override def process(context: IContext) = context + (p2 -> "Test2")
    }

    val t3 = new TestTask {
      val name = "Test read conjonctive"
      override def inputs = DataSet(p1, p2.toArray)
      override def process(context: IContext) = {
        context.value(p1).get should equal("Test1")
        context.value(p2.toArray).get.head should equal("Test2")
        context.value(p2.toArray).get.size should equal(100)
        executed += 1
        context
      }
    }

    val initc = new Capsule(init)
    val t1c = new Capsule(t1)

    val t3c = new Capsule(t3)

    val t2CList = (0 until 100).map {
      i ⇒
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
      t2CList.map { _ -> new FixedEnvironmentSelection(env) }.toMap).start.waitUntilEnded
    executed should equal(1)
  }

  "A conjonctive pattern" should "aggregate array variable of the same name in an array of array of the closest common supertype" in {

    val p1 = new Prototype[Array[java.lang.Long]]("p")
    val p2 = new Prototype[Array[java.lang.Integer]]("p")
    val pArray = new Prototype[Array[Array[java.lang.Number]]]("p")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: IContext) = context + (p1 -> Array(new java.lang.Long(1L)))
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: IContext) = context + (p2 -> Array(new java.lang.Integer(2)))
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(pArray)
      override def process(context: IContext) = {
        val res = IndexedSeq(context.value(pArray).get(0).deep, context.value(pArray).get(1).deep)

        res.contains(Array(new java.lang.Integer(1)).deep) should equal(true)
        res.contains(Array(new java.lang.Long(2L)).deep) should equal(true)

        context.value(pArray).get.getClass should equal(classOf[Array[Array[java.lang.Number]]])
        context
      }
    }

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
