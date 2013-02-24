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

import org.openmole.core.implementation.execution.local._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.data._
import org.openmole.core.model.transition._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TransitionSpec extends FlatSpec with ShouldMatchers {

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = Prototype[String]("p")

    val t1 = new TestTask {
      val name = "Test write"
      override def outputs = DataSet(p)
      override def process(context: Context) = context + (p -> "Test")
    }

    val t2 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p)
      override def process(context: Context) = {
        context.value(p).get should equal("Test")
        context
      }
    }

    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)

    (t1c -- t2c).start.waitUntilEnded
  }

  "A conjonctive pattern" should "enable variable values to be transmitted from a task to another" in {
    val p1 = Prototype[String]("p1")
    val p2 = Prototype[String]("p2")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: Context) = context + (p1 -> "Test1")
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: Context) = context + (p2 -> "Test2")
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(p1, p2)
      override def process(context: Context) = {
        context.value(p1).get should equal("Test1")
        context.value(p2).get should equal("Test2")
        context
      }
    }

    val initc = new Capsule(init)
    val t1c = new Capsule(t1)
    val t2c = new Capsule(t2)
    val t3c = Slot(Capsule(t3))

    val ex = (initc -- t1c -- t3c) + (initc -- t2c -- t3c)

    ex.start.waitUntilEnded

  }

  "A conjonctive pattern" should "aggregate variable of the same name in an array of closest common supertype" in {

    val p1 = Prototype[java.lang.Long]("p")
    val p2 = Prototype[java.lang.Integer]("p")
    val pArray = Prototype[Array[java.lang.Number]]("p")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: Context) = context + (p1 -> new java.lang.Long(1L))
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: Context) = context + (p2 -> new java.lang.Integer(2))
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(pArray)
      override def process(context: Context) = {
        //println(context.value(pArtoStringray).map(_.intL))
        context.value(pArray).get.map(_.intValue).contains(1) should equal(true)
        context.value(pArray).get.map(_.intValue).contains(2) should equal(true)

        context.value(pArray).get.getClass should equal(classOf[Array[java.lang.Number]])
        context
      }
    }

    val initc = Capsule(init)
    val t1c = Capsule(t1)
    val t2c = Capsule(t2)
    val t3c = Slot(t3)

    val ex = (initc -- t1c -- t3c) + (initc -- t2c -- t3c)
    ex.start.waitUntilEnded

  }

  "A conjonctive pattern" should "be robust to concurent execution" in {
    @volatile var executed = 0
    val p1 = Prototype[String]("p1")
    val p2 = Prototype[String]("p2")

    val init = EmptyTask("Init conjonctive")

    val t1 = new TestTask {
      val name = "Test write 1 conjonctive"
      override def outputs = DataSet(p1)
      override def process(context: Context) = context + (p1 -> "Test1")
    }

    val t2 = new TestTask {
      val name = "Test write 2 conjonctive"
      override def outputs = DataSet(p2)
      override def process(context: Context) = context + (p2 -> "Test2")
    }

    val t3 = new TestTask {
      val name = "Test read conjonctive"
      override def inputs = DataSet(p1, p2.toArray)
      override def process(context: Context) = {
        context.value(p1).get should equal("Test1")
        context.value(p2.toArray).get.head should equal("Test2")
        context.value(p2.toArray).get.size should equal(100)
        executed += 1
        context
      }
    }

    val initc = new Capsule(init)
    val t1c = new Capsule(t1)

    val t3c = Slot(t3)

    val env = new LocalEnvironment(20)

    val mole = initc -- t1c -- t3c + (0 until 100).map {
      i ⇒ initc -- (Capsule(t2) on env) -- t3c
    }.reduce(_ + _)

    mole.start.waitUntilEnded
    executed should equal(1)
  }

  "A conjonctive pattern" should "aggregate array variable of the same name in an array of array of the closest common supertype" in {

    val p1 = Prototype[Array[java.lang.Long]]("p")
    val p2 = Prototype[Array[java.lang.Integer]]("p")
    val pArray = Prototype[Array[Array[java.lang.Number]]]("p")

    val init = EmptyTask("Init")

    val t1 = new TestTask {
      val name = "Test write 1"
      override def outputs = DataSet(p1)
      override def process(context: Context) = context + (p1 -> Array(new java.lang.Long(1L)))
    }

    val t2 = new TestTask {
      val name = "Test write 2"
      override def outputs = DataSet(p2)
      override def process(context: Context) = context + (p2 -> Array(new java.lang.Integer(2)))
    }

    val t3 = new TestTask {
      val name = "Test read"
      override def inputs = DataSet(pArray)
      override def process(context: Context) = {
        val res = IndexedSeq(context.value(pArray).get(0).deep, context.value(pArray).get(1).deep)

        res.contains(Array(new java.lang.Integer(1)).deep) should equal(true)
        res.contains(Array(new java.lang.Long(2L)).deep) should equal(true)

        context.value(pArray).get.getClass should equal(classOf[Array[Array[java.lang.Number]]])
        context
      }
    }

    val initc = Capsule(init)
    val t1c = Capsule(t1)
    val t2c = Capsule(t2)
    val t3c = Slot(t3)

    val mole = (initc -- t1c -- t3c) + (initc -- t2c -- t3c)
    mole.start.waitUntilEnded
  }

}
