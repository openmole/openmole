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

package org.openmole.core.workflow.puzzle

import java.util.concurrent.atomic.AtomicBoolean

import org.openmole.core.context.Val
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.execution.LocalEnvironment
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.test.TestTask
import org.openmole.core.workflow.transition.TransitionSlot
import org.openmole.core.workflow.validation.Validation
import org.scalatest._

class TransitionSpec extends flatspec.AnyFlatSpec with matchers.should.Matchers {

  import org.openmole.core.workflow.test.Stubs._

  "Transitions" should "compile" in {
    EmptyTask() -- EmptyTask()
    Capsule(EmptyTask()) -- Capsule(EmptyTask())
  }

  "Multiple transitions" should "converge toward the same slot" in {
    val p1 = Val[Int]
    val p2 = Val[Int]

    val t1 = EmptyTask() set (outputs += p1)
    val t2 = EmptyTask() set (outputs += p2)
    val t3 = EmptyTask() set (inputs += (p1, p2))

    val wf = EmptyTask() -- (t1, t2) -- t3

    Validation(wf).isEmpty should equal(true)
  }

  "A transition" should "enable variable values to be transmitted from a task to another" in {
    val p = Val[String]("p")
    val t1Executed = new AtomicBoolean(false)
    val t2Executed = new AtomicBoolean(false)

    val t1 = TestTask { ctx =>
      t1Executed.set(true)
      ctx + (p -> "Test")
    } set (
      name := "Test write",
      outputs += p
    )

    val t2 = TestTask { context =>
      t2Executed.set(true)
      context(p) should equal("Test")
      context
    } set (
      name := "Test read",
      inputs += p
    )

    (t1 -- t2).run()

    t1Executed.get should equal(true)
    t2Executed.get should equal(true)
  }

  "A conjonctive pattern" should "enable variable values to be transmitted from a task to another" in {
    val p1 = Val[String]("p1")
    val p2 = Val[String]("p2")

    val init = EmptyTask()

    val t1 = TestTask { _ + (p1 -> "Test1") } set (outputs += p1)
    val t2 = TestTask { _ + (p2 -> "Test2") } set (outputs += p2)

    val t3 = TestTask { context =>
      context(p1) should equal("Test1")
      context(p2) should equal("Test2")
      context
    } set (inputs += (p1, p2))

    val ex = (init -- t1 -- t3) & (init -- t2 -- t3)

    ex.run()
  }

  "Filter" should "prevent a variable from going through a transition" in {
    val p1 = Val[Int]

    val t1 = EmptyTask() set (outputs += p1)
    val t2 = EmptyTask() set (inputs += p1)

    Validation((t1 -- t2 block p1) -- t1).isEmpty should equal(false)
  }

  "A conjonctive pattern" should "be robust to concurrent execution" in {
    @volatile var executed = 0
    val p1 = Val[String]("p1")
    val p2 = Val[String]("p2")

    val init = EmptyTask()

    val t1 = TestTask { _ + (p1 -> "Test1") } set (outputs += p1)

    val t2 = TestTask { _ + (p2 -> "Test2") } set (outputs += p2)

    val t3 = TestTask { context =>
      context(p1) should equal("Test1")
      context(p2.toArray).head should equal("Test2")
      context(p2.toArray).size should equal(100)
      executed += 1
      context
    } set (inputs += (p1, p2.array))

    val env = LocalEnvironment(20)

    val mole =
      init -- (t1 on env) -- t3 &
        (0 until 100).map {
          i => init -- Capsule(t2 on env) -- t3
        }.reduce[DSL](_ & _)

    mole.run()
    executed should equal(1)
  }

  "Transition syntax" should "should support a sequence task as parameter" in {
    val t1 = EmptyTask()
    val to = Seq.fill(3)(EmptyTask())
    t1 -- to
  }

}
