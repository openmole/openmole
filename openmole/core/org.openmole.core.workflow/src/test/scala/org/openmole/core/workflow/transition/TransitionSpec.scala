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

import java.util.concurrent.atomic.AtomicBoolean

import org.openmole.core.context.Val
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.builder._
import org.scalatest._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.dsl._

class TransitionSpec extends FlatSpec with Matchers {

  import org.openmole.core.workflow.tools.StubServices._

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = Val[String]("p")
    val t1Executed = new AtomicBoolean(false)
    val t2Executed = new AtomicBoolean(false)

    val t1 = TestTask { ctx ⇒
      t1Executed.set(true)
      ctx + (p → "Test")
    } set (
      name := "Test write",
      outputs += p
    )

    val t2 = TestTask { context ⇒
      t2Executed.set(true)
      context(p) should equal("Test")
      context
    } set (
      name := "Test read",
      inputs += p
    )

    val t1c = Capsule(t1)
    val t2c = Capsule(t2)

    (t1c -- t2c).run()

    t1Executed.get should equal(true)
    t2Executed.get should equal(true)
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

    ex.run()

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

    mole.toExecution(defaultEnvironment = LocalEnvironment(20)).run()
    executed should equal(1)
  }

  "Transition syntax" should "should support a sequence task as parameter" in {
    val t1 = EmptyTask()
    val to = Seq.fill(3)(EmptyTask())
    t1 -- (to: _*)
  }

}
