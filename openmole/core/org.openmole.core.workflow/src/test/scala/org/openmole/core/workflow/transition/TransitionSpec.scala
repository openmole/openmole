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

package org.openmole.core.workflow.transition

import org.openmole.core.context.Val
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.builder._
import org.scalatest._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.dsl._

class TransitionSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = Val[String]("p")

    val t1 = TestTask { _ + (p → "Test") } set (
      name := "Test write",
      outputs += p
    )

    val t2 = TestTask { context ⇒
      context(p) should equal("Test")
      context
    } set (
      name := "Test read",
      inputs += p
    )

    val t1c = Capsule(t1)
    val t2c = Capsule(t2)

    (t1c -- t2c).start.waitUntilEnded
  }

  "A conjonctive pattern" should "enable variable values to be transmitted from a task to another" in {
    val p1 = Val[String]("p1")
    val p2 = Val[String]("p2")

    val init = EmptyTask()

    val t1 = TestTask { _ + (p1 → "Test1") } set (
      name := "Test write 1",
      outputs += p1
    )

    val t2 = TestTask { _ + (p2 → "Test2") } set (
      name := "Test write 2",
      outputs += p2
    )

    val t3 = TestTask { context ⇒
      context(p1) should equal("Test1")
      context(p2) should equal("Test2")
      context
    } set (
      name := "Test read",
      inputs += (p1, p2)
    )

    val initc = Capsule(init)
    val t1c = Capsule(t1)
    val t2c = Capsule(t2)
    val t3c = Slot(Capsule(t3))

    val ex = (initc -- t1c -- t3c) & (initc -- t2c -- t3c)

    ex.start.waitUntilEnded

  }

  "A conjonctive pattern" should "be robust to concurrent execution" in {
    @volatile var executed = 0
    val p1 = Val[String]("p1")
    val p2 = Val[String]("p2")

    val init = EmptyTask()

    val t1 = TestTask { _ + (p1 → "Test1") } set (
      name := "Test write 1 conjonctive",
      outputs += p1
    )

    val t2 = TestTask { _ + (p2 → "Test2") } set (
      name := "Test write 2 conjonctive",
      outputs += p2
    )

    val t3 = TestTask { context ⇒
      context(p1) should equal("Test1")
      context(p2.toArray).head should equal("Test2")
      context(p2.toArray).size should equal(100)
      executed += 1
      context
    } set (
      name := "Test read conjonctive",
      inputs += (p1, p2.array)
    )

    val initc = Capsule(init)
    val t1c = Capsule(t1)

    val t3c = Slot(t3)

    val mole = initc -- t1c -- t3c & (0 until 100).map {
      i ⇒ initc -- t2 -- t3c
    }.reduce(_ & _)

    mole.toExecution(defaultEnvironment = LocalEnvironment(20)).start.waitUntilEnded
    executed should equal(1)
  }

  "Transition syntax" should "should support a sequence task as parameter" in {
    val t1 = EmptyTask()
    val to = Seq.fill(3)(EmptyTask())
    t1 -- (to: _*)
  }

  /*

    - NOTE: This spec has been revoked due to the impossibility of achieving it thoroughly in the general case and the relatively
    low usage of it in all the workflow developed until now

    A conjonctive pattern" should "aggregate variable of the same name in an array of closest common supertype" in {

      val p1 = Prototype[java.lang.Long]("p")
      val p2 = Prototype[java.lang.Integer]("p")
      val pArray = Prototype[Array[java.lang.Number]]("p")

      val init = EmptyTask()

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
          context(pArray).map(_.intValue).contains(1) should equal(true)
          context(pArray).map(_.intValue).contains(2) should equal(true)

          context(pArray).getClass should equal(classOf[Array[java.lang.Number]])
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


    "A conjonctive pattern" should "aggregate array variable of the same name in an array of array of the closest common supertype" in {

      val p1 = Prototype[Array[java.lang.Long]]("p")
      val p2 = Prototype[Array[java.lang.Integer]]("p")
      val pArray = Prototype[Array[Array[java.lang.Number]]]("p")

      val init = EmptyTask()

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
          val res = IndexedSeq(context(pArray)(0).deep, context(pArray)(1).deep)

          res.contains(Array(new java.lang.Integer(1)).deep) should equal(true)
          res.contains(Array(new java.lang.Long(2L)).deep) should equal(true)

          context(pArray).getClass should equal(classOf[Array[Array[java.lang.Number]]])
          context
        }
      }

      val initc = Capsule(init)
      val t1c = Capsule(t1)
      val t2c = Capsule(t2)
      val t3c = Slot(t3)

      val mole = (initc -- t1c -- t3c) + (initc -- t2c -- t3c)
      mole.start.waitUntilEnded
    }*/

}
